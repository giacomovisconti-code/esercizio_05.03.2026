package org.example.productservice.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PostMapping("/inventory/create/{productId}")
    ResponseEntity<String> createStock(@PathVariable("productId") UUID productId);

    @DeleteMapping("/inventory/delete/{productId}")
    ResponseEntity<String> deleteStock(@PathVariable("productId") UUID productId);
}
