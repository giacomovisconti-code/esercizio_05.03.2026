package org.example.productservice.product;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import org.example.productservice.dto.ProductDto;
import org.example.productservice.entities.Product;
import org.example.productservice.exceptions.Errors;
import org.example.productservice.exceptions.ProductException;
import org.example.productservice.openfeign.InventoryClient;
import org.example.productservice.repositories.ProductRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductTest {

    static Network network = Network.newNetwork();

    @Autowired
    ProductRepository productRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryClient inventoryClient;

    @Autowired
    private ObjectMapper objectMapper;

    // Creo un container su cui faccio girare il mio db fittizio
    @Container
    static MySQLContainer<?> mysqlConainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("test")
            .withUsername("mysql")
            .withPassword("mysql")
            .withNetwork(network);

    @Container
    static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:8.6.2-alpine"))
            .withExposedPorts(6379)
            .withNetwork(network);


    @DynamicPropertySource
    static void configureMysqlProp(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlConainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlConainer::getUsername);
        registry.add("spring.datasource.password", mysqlConainer::getPassword);

        //JPA-HIBERNATE
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");

        // REDIS CONFIG
        registry.add("spring.data.redis.port", ()-> redisContainer.getMappedPort(6379));
        registry.add("spring.data.redis.host", redisContainer::getHost);
    }

    // Creo un unico UUID condiviso con gli altri test
    private static final UUID productSku = UUID.randomUUID();

    @Test
    @Order(1)
    void connectionEst(){
        Assertions.assertTrue(mysqlConainer.isCreated());
        Assertions.assertTrue(mysqlConainer.isRunning());
        Assertions.assertTrue(redisContainer.isCreated());
        Assertions.assertTrue(redisContainer.isRunning());

    }

    @Test
    @Order(2)
    void createProductShouldReturnOk() throws Exception {
        Mockito.when(inventoryClient.createStock(Mockito.any()))
                .thenReturn(ResponseEntity.ok().build());

        String json = """
                {
                  "name": "Smartphone Super Pro",
                  "sku": "%s",
                  "price": 899.99,
                  "description": "Un fantastico smartphone con display OLED e tripla fotocamera."
                }
                """.formatted(productSku);
        System.out.println(productSku);

        MvcResult result = mockMvc.perform(
                post("/products/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        // Verifico che sia stato eseguita la chiamata verso l'altro microservice
        Mockito.verify(inventoryClient, Mockito.times(1)).createStock(productSku);
        System.out.println(productSku);

        String res = result.getResponse().getContentAsString();
        System.out.println(res);

        System.out.println(productSku);
        Product p = productRepository.findBySku(productSku).orElseThrow(() -> new ProductException(Errors.PRODUCT_NOT_FOUND.key(), Errors.PRODUCT_NOT_FOUND.message()));
        System.out.println(p);
        System.out.println(productSku);
        Assertions.assertNotNull(p.getSku());
    }

    @Test
    @Order(3)
    void searchProductByNameShouldReturnOk() throws Exception {
//        System.out.println(productSku);
//        Product p1 = productRepository.findBySku(productSku).orElseThrow(() -> new ProductException(Errors.PRODUCT_NOT_FOUND.key(), Errors.PRODUCT_NOT_FOUND.message()));
//        System.out.println(p1);
//
        // Salvo in una variabile il nome del prodotto appena creato
        String name = "Smartphone Super Pro";

        // Simulo la chiamata API all'endpint preposto
        MvcResult result = mockMvc.perform(
                get("/products/search")
                        .queryParam("name", name))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(res);
        List<ProductDto> p = objectMapper.readValue(json.get("content").traverse(), new TypeReference<>(){});
        System.out.println(p);

        // Verifico che il nome del prodotto che mi aspetto coincida con quello effettivo
        Assertions.assertEquals(name, p.get(0).getName());
        // Che nella lista di prodotti ritornati ci sia solo il prodotto inserito
        Assertions.assertEquals(1, p.size());
        Assertions.assertEquals(productSku, p.get(0).getSku());
    }

    @Test
    @Order(4)
    void getSingleProductShouldReturnOk() throws  Exception{


        MvcResult result = mockMvc.perform(
                get("/products/{sku}", productSku))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        ProductDto p = objectMapper.readValue(res, new TypeReference<>() {});

        System.out.println(p);

        Assertions.assertNotNull(p);
        Assertions.assertEquals(productSku, p.getSku());

    }

    @Test
    @Order(5)
    void updateProductShouldReturnOk() throws  Exception{

        // Modifico il prodotto "Super" -> "Mega", "899.99" -> "699.99"
        String json = """
                {
                  "name": "Smartphone Mega Pro",
                  "sku": "%s",
                  "price": 699.99,
                  "description": "Un fantastico smartphone con display OLED e tripla fotocamera."
                }
                """.formatted(productSku);

        // Simulo la chiamata API
        MvcResult result = mockMvc.perform(
                patch("/products/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        System.out.println(res);

        Optional<Product> p = productRepository.findBySku(productSku);
        if (p.isEmpty()) throw new Exception("Product not found");

        Product product = p.get();
        System.out.println(product);

        // Verifico le modifiche
        Assertions.assertEquals("Smartphone Mega Pro", product.getName());
        Assertions.assertEquals(BigDecimal.valueOf(699.99), product.getPrice());

    }

    @Test
    @Order(6)
    void deleteProductShouldReturnOK() throws Exception{

        MvcResult result = mockMvc.perform(
                delete("/products/delete/{sku}", productSku))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();

        System.out.println(res);

        Optional<Product> pOpt = productRepository.findBySku(productSku);

        Assertions.assertTrue(pOpt.isEmpty(), "Prodotto ancora presente");

    }
}
