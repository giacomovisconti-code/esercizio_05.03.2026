package org.example.orderservice.openfeign;

import org.example.orderservice.dto.StockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "inventory-service", url = "localhost:8081", configuration = FeignClientConfiguration.class)
public interface InventoryClient {
    @GetMapping("/inventory/{productId}")
    public ResponseEntity<StockRequest> getStock(@PathVariable("productId") UUID productId);

    @PatchMapping("/inventory/deduction/{productId}")
    public ResponseEntity<String> deductionStock(@PathVariable("productId") UUID productId, @RequestParam("quantity") Long quantity);

    @PatchMapping("/inventory/addition/{sku}")
    public ResponseEntity<String> additionStock(@PathVariable("sku") UUID sku, @RequestParam("quantity") Long quantity);
}
