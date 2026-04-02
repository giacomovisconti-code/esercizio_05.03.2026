package org.example.orderservice.order;


import com.redis.testcontainers.RedisContainer;
import jakarta.transaction.Transactional;
import org.example.orderservice.dto.ProductDto;
import org.example.orderservice.dto.StockRequest;
import org.example.orderservice.exceptions.Errors;
import org.example.orderservice.exceptions.OrderException;
import org.example.orderservice.openfeign.InventoryClient;
import org.example.orderservice.openfeign.ProductClient;
import org.example.orderservice.repositories.OrderRepository;
import org.example.orderservice.entities.Order;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static java.math.BigDecimal.valueOf;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false",
                "spring.cloud.config.enabled=false",
                "spring.config.import="
        }
)
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

    @Container
    static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:8.6.2-alpine"))
            .withExposedPorts(6379)
            .withNetwork(network);

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

        System.out.println("Kafka Bootstrap:" + kafkaContainer.getBootstrapServers());

        registry.add("spring.kafka.bootstrap-servers", ()-> kafkaBootstrapSevers);
        registry.add("spring.kafka.producer.key-serializer", ()-> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", ()-> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.key-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");

        registry.add("spring.data.redis.port", ()-> redisContainer.getMappedPort(6379));
        registry.add("spring.data.redis.host", redisContainer::getHost);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private ProductClient productClient;

    private final UUID userId = UUID.randomUUID();

    private static UUID singleProductOrderId;

    @Test
    @org.junit.jupiter.api.Order(1)
    void connectionEst(){
        Assertions.assertTrue(mySQLContainer.isCreated());
        Assertions.assertTrue(mySQLContainer.isRunning());
        Assertions.assertTrue(kafkaContainer.isCreated());
        Assertions.assertTrue(kafkaContainer.isRunning());
        Assertions.assertTrue(redisContainer.isRunning());
        Assertions.assertTrue(redisContainer.isCreated());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void createOrderWithSingleProductShouldReturnOk() throws Exception {
        // Setto dei dati in entrata
        UUID sku = UUID.randomUUID();
        BigDecimal productPrice = valueOf(299.50);

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

        long qty = 2L;
        // Creo il json da passare per la creazione
        String json = """
                [
                    {
                        "sku":"%s",
                        "quantity": %d
                    }
                ]
                """.formatted(sku, qty);

        MvcResult result = mockMvc.perform(post("/orders/create", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        System.out.println(res);
        Order o = objectMapper.readValue(res, Order.class);
        // Imposto la paginazione del json risultante

        System.out.println(o.getId());

        singleProductOrderId = o.getId();

        // Salvo il totale dell'ordine che mi aspetto in una variabile
        BigDecimal total = productPrice.multiply(valueOf(qty));
        Assertions.assertEquals(1, o.getOrderItems().size());
        Assertions.assertEquals(total.stripTrailingZeros(),o.getTotal().stripTrailingZeros());
    }


    @Test
    @org.junit.jupiter.api.Order(3)
    public void createOrderWithMultipleProductsShouldReturnOk() throws Exception {
        // Setto dei dati in entrata
        // sku dei prodotti
        UUID sku = UUID.randomUUID();
        UUID sku1 = UUID.randomUUID();
        UUID sku2 = UUID.randomUUID();

        // Prezzi dei prodotti
        BigDecimal productPrice = valueOf(299.50);
        BigDecimal productPrice1 = valueOf(199.50);
        BigDecimal productPrice2 = valueOf(99.50);

        // Creo i prodotti per simulare OpenFeign
        ProductDto p = new ProductDto();
        p.setSku(sku);
        p.setPrice(productPrice);
        ProductDto p1 = new ProductDto();
        p1.setSku(sku1);
        p1.setPrice(productPrice1);
        ProductDto p2 = new ProductDto();
        p2.setSku(sku2);
        p2.setPrice(productPrice2);

        // Simulo la chiamata OpenFeign
        when(productClient.getProduct(sku))
                .thenReturn(ResponseEntity.ok(p));
        when(productClient.getProduct(sku1))
                .thenReturn(ResponseEntity.ok(p1));
        when(productClient.getProduct(sku2))
                .thenReturn(ResponseEntity.ok(p2));

        // Creo la giacenza dei prodotti
        StockRequest stock = new StockRequest();
        stock.setQuantity(2L);
        stock.setSku(sku);
        StockRequest stock1 = new StockRequest();
        stock1.setQuantity(2L);
        stock1.setSku(sku1);
        StockRequest stock2 = new StockRequest();
        stock2.setQuantity(2L);
        stock2.setSku(sku2);

        // Setto lo stock dei prodotti da ordinare
        when(inventoryClient.getStock(sku))
                .thenReturn(ResponseEntity.ok(stock));
        when(inventoryClient.getStock(sku1))
                .thenReturn(ResponseEntity.ok(stock1));
        when(inventoryClient.getStock(sku2))
                .thenReturn(ResponseEntity.ok(stock2));

        // Imposto le quantità dei prodotti da ordinare
        long qty = 1L;
        long qty1 = 1L;
        long qty2 = 2L;

        // Creo il json da passare per la creazione
        String json = """
                [
                    {
                        "sku":"%s",
                        "quantity": %d
                    },
                     {
                        "sku":"%s",
                        "quantity": %d
                    },
                     {
                        "sku":"%s",
                        "quantity": %d
                    }
                ]
                """.formatted(sku, qty, sku1, qty1, sku2, qty2);

        // Simulo la chiamata rest
        MvcResult result = mockMvc.perform(post("/orders/create", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        System.out.println(res);
        Order o = objectMapper.readValue(res, Order.class);
        // Calcolo il totale ordine atteso
        BigDecimal total = productPrice.multiply(valueOf(qty))
                .add(productPrice1.multiply(valueOf(qty1)))
                .add(productPrice2.multiply(valueOf(qty2)));

        // Salvo in una variabile orderId

        // Verifico che il totale atteso sia uguale a quello calcolato dallo script
        Assertions.assertEquals(0, total.compareTo(o.getTotal()));
        Assertions.assertEquals(3, o.getOrderItems().size());

    }


    @Test
    @org.junit.jupiter.api.Order(4)
    void updateSingleProductOrderStatusToOthersShouldReturnBadRequest() throws Exception {
        System.out.println(singleProductOrderId);
        // Aggiorno lo stato dell'ordine a "confermato"
        Order o = orderRepository.findOrderById(singleProductOrderId).orElseThrow(()-> new OrderException(Errors.ORDER_NOT_FOUND.key(), Errors.ORDER_NOT_FOUND.message()));

        // Verifico che l'ordine appena creato abbia lo status di Bozza
        Assertions.assertEquals("BOZZA", o.getOrderStatus().toString());

        // Cambio lo stato dell'ordine in un ordine diverso da quello imposto dalla logica BOZZA -> CONFERMATO -> IN_LAVORAZIONE -> EMESSO
        changeOrderStatusShouldReturnBadRequest(o.getId(), "IN_LAVORAZIONE");
        changeOrderStatusShouldReturnBadRequest(o.getId(), "EMESSO");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @Transactional
    void updateSingleProductOrderStatusToConfirmShouldReturnOk() throws Exception {

        System.out.println(singleProductOrderId);
        // Aggiorno lo stato dell'ordine a "Confermato"
        Order o = orderRepository.findOrderById(singleProductOrderId).orElseThrow(()-> new OrderException(Errors.ORDER_NOT_FOUND.key(), Errors.ORDER_NOT_FOUND.message()));
        Assertions.assertEquals("BOZZA", o.getOrderStatus().toString());
        MvcResult result = mockMvc.perform(patch("/orders/changestatus/{orderId}", o.getId())
                        .param("status","CONFERMATO"))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();

        Order orderUpdated = objectMapper.readValue(res, Order.class);

        // Verifico che l'ordine appena creato abbia lo status di Bozza
        Assertions.assertEquals("CONFERMATO", orderUpdated.getOrderStatus().toString());
    }


    @Test
    @org.junit.jupiter.api.Order(6)
    @Transactional
    void deactivateOrderShouldReturnOk() throws Exception {

        MvcResult result = mockMvc.perform( patch("/orders/deactivate/{orderId}", singleProductOrderId))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        Order o = objectMapper.readValue(res, Order.class);

        Assertions.assertFalse(o.getActive());
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    void reactivateOrderShouldReturnOk() throws Exception {

        MvcResult result = mockMvc.perform( patch("/orders/reactivate/{orderId}", singleProductOrderId))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        Order o = objectMapper.readValue(res, Order.class);

        Assertions.assertTrue(o.getActive());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    void deleteOrderShouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(patch("/orders/delete/{orderId}", singleProductOrderId))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        System.out.println(res);
        Order o = orderRepository.findOrderById(singleProductOrderId).orElseThrow(()->new OrderException(Errors.ORDER_NOT_FOUND.key(), Errors.ORDER_NOT_FOUND.message()));
        Pageable pageable = PageRequest.of(0,9);
        Page<Order> ls = orderRepository.findAllByDeletedFalse(pageable);
        Assertions.assertEquals(1, ls.getTotalElements());
        Assertions.assertTrue(o.getDeleted());
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    void reactiveDeletedOrderShouldReturnBadRequest() throws Exception {
        MvcResult result = mockMvc.perform( patch("/orders/reactivate/{orderId}", singleProductOrderId))
                .andExpect(status().isBadRequest())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        System.out.println(res);
    }


    //? UTILS
    private void changeOrderStatusShouldReturnBadRequest(UUID orderId, String status) throws Exception {

        MvcResult result = mockMvc.perform(patch("/orders/changestatus/{orderId}", orderId)
                        .param("status",status))
                .andExpect(status().isBadRequest())
                .andReturn();
        String res = result.getResponse().getContentAsString();

        Order orderUpdated = objectMapper.readValue(res, Order.class);

        Assertions.assertEquals("BOZZA", orderUpdated.getOrderStatus().toString());
    }



}
