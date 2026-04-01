package org.example.inventoryservice.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import org.example.inventoryservice.dto.StockRequest;
import org.example.inventoryservice.entities.Inventory;
import org.example.inventoryservice.repositories.InventoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryTest {

    @Autowired
    InventoryRepository inventoryRepository;

    private static final Network network = Network.newNetwork();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("test")
            .withUsername("mysql")
            .withPassword("mysql")
            .withNetwork(network);

    @Container
    static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:8.6.2-alpine"))
            .withExposedPorts(6379)
            .withNetwork(network);

    @DynamicPropertySource
    static void configureMysqlProp(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);

        //JPA-HIBERNATE
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @DynamicPropertySource
    static void configureRedisProp(DynamicPropertyRegistry registry){
        registry.add("spring.data.redis.port", ()-> redisContainer.getMappedPort(6379));
        registry.add("spring.data.redis.host", redisContainer::getHost);
    }

    private static UUID sku = UUID.randomUUID();



    @Test
    @Order(1)
    void connectionEst(){
        Assertions.assertTrue(mySQLContainer.isCreated());
        Assertions.assertTrue(mySQLContainer.isRunning());
    }

    @Test
    @Order(2)
    void createStock(){

        Inventory stock = new Inventory();
        stock.setQuantity(0L);
        stock.setSku(sku);
        inventoryRepository.save(stock);
        assertTrue(inventoryRepository.findBySku(sku).isPresent());
    }

    @Test
    @Order(3)
    void getAllStocksShouldReturn1() throws  Exception{

        MvcResult result = mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(res);

     long total = json.get("totalElements").asLong();
        System.out.println(json);
        assertEquals(1, total);

    }

    @Test
    @Order(4)
    void getSingleStockShouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(get("/inventory/{productId}", sku))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();

        StockRequest stock = objectMapper.readValue(res, new TypeReference<>(){});
        System.out.println(stock);

        assertEquals(sku, stock.getSku());

    }

    @Test
    @Order(5)
    void updateStockShouldChange() throws Exception {
        String json = """
                    {
                        "sku":"%s",
                        "quantity": 5
                    }
                """.formatted(sku);

        MvcResult result = mockMvc.perform(
                patch("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();

        System.out.println(res);

        Inventory inv = inventoryRepository.findBySku(sku).orElseThrow(()-> new Exception("Stock non trovato"));

        assertTrue(inv.getQuantity() == 5L);
    }

    @Test
    @Order(6)
    void deductStockQuantityShouldChange() throws Exception {
        // test dell'endpoint gestito da OpenFeign

        String json = """
                [
                    {
                        "sku":"%s",
                        "quantity": 3
                    }
                ]
                """.formatted(sku);
        MvcResult result = mockMvc.perform(
                put("/inventory/deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();

        Inventory stock = inventoryRepository.findBySku(sku).orElseThrow(() -> new Exception("Stock non trovato"));

        System.out.println(res);
        System.out.println(stock);

        assertTrue(stock.getQuantity() == 2L);
    }

    @Test
    @Order(7)
    void addStockQuantityShouldChange() throws Exception {
        // test dell'endpoint gestito da OpenFeign
        String json = """
                [
                    {
                        "sku":"%s",
                        "quantity": 3
                    }
                ]
                """.formatted(sku);

        MvcResult result = mockMvc.perform(
                        put("/inventory/addition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();

        Inventory stock = inventoryRepository.findBySku(sku).orElseThrow(() -> new Exception("Stock non trovato"));

        System.out.println(res);
        System.out.println(stock);

        assertTrue(stock.getQuantity() == 5L);
    }

    @Test
    @Order(8)
    void deleteStockShouldReturnOk() throws Exception{
        MvcResult result = mockMvc.perform(
                delete("/inventory/delete/{productId}", sku))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();

        Optional<Inventory> stockOpt = inventoryRepository.findBySku(sku);

        assertTrue(stockOpt.isEmpty());
    }
}
