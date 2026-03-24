package org.example.orderservice.order;


import org.example.orderservice.dto.ProductDto;
import org.example.orderservice.dto.StockRequest;
import org.example.orderservice.openfeign.InventoryClient;
import org.example.orderservice.openfeign.ProductClient;
import org.example.orderservice.repositories.OrderRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderTest {

    private static final Network network = Network.newNetwork();

    @Autowired
    OrderRepository  orderRepository;

    // Creo il container per il DB mysql
    @Container
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("test")
            .withUsername("mysql")
            .withPassword("mysql")
            .withNetwork(network);

    // Creo il container su cui far girare Kafka
    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(network)
            .withListener(()->"kafka:19092")
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofMinutes(2)));

    // Setto le props
    @DynamicPropertySource
    static void configureMProps(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);

        //JPA-HIBERNATE
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");

        // KAFKA-CONFIG
        String kafkaBootstrapSevers = kafkaContainer.getBootstrapServers();

        registry.add("spring.kafka.bootstrap-servers", ()-> kafkaBootstrapSevers);
        registry.add("spring.kafka.producer.key-serializer", ()-> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", ()-> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.key-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private ProductClient productClient;

    private UUID userId = UUID.randomUUID();
    @Test
    @Order(1)
    void connectionEst(){
        Assertions.assertTrue(mySQLContainer.isCreated());
        Assertions.assertTrue(mySQLContainer.isRunning());
        Assertions.assertTrue(kafkaContainer.isCreated());
        Assertions.assertTrue(kafkaContainer.isRunning());
    }

    @Test
    @Order(2)
    void createOrderWithSingleProductShouldReturnOk() throws Exception {
        // Setto dei dati in input
        UUID sku = UUID.randomUUID();
        BigDecimal productPrice = BigDecimal.valueOf(299.50);

        // Creo il prodotto per simulare OpenFeign
        ProductDto p = new ProductDto();
        p.setSku(sku);
        p.setPrice(productPrice);

        // Simulo la chiamata OpenFeign
        when(productClient.getProduct(sku))
                .thenReturn(ResponseEntity.ok(p));

        // creo la giacenza del prodotto per simulare OpenFeign
        StockRequest stock = new StockRequest();
        stock.setQuantity(5L);
        stock.setSku(sku);

        when(inventoryClient.getStock(sku))
                .thenReturn(ResponseEntity.ok(stock));


        // Creo il json da passare per la creazione
        String json = """
                [
                    {
                        "sku":"%s",
                        "quantity": 2
                    }
                ]k
                """.formatted(sku);

        MvcResult result = mockMvc.perform(post("/orders/create/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        Pageable pageable = PageRequest.of(1,0);

        System.out.println(res);
        Assertions.assertFalse(orderRepository.findAllByDeletedFalse(pageable).isEmpty());
        Assertions.assertTrue(orderRepository.findAllByDeletedFalse(pageable).getTotalElements() == 1);
    }





}
