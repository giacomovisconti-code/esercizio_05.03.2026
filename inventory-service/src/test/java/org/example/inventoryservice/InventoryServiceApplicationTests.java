package org.example.inventoryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootTest
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
class InventoryServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
