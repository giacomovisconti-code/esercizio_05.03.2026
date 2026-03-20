package org.example.orderservice.openfeign;

import jakarta.validation.Valid;
import org.example.orderservice.dto.StockChange;
import org.example.orderservice.dto.StockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "inventory-service")
public interface InventoryClient {
    @GetMapping("/inventory/{productId}")
    public ResponseEntity<StockRequest> getStock(@PathVariable("productId") UUID productId);

    @PutMapping("/deduction")
    public ResponseEntity<String> deductionStock(@Valid @RequestBody List<StockChange> list);

    @PutMapping("/addition")
    public ResponseEntity<String> additionStock(@Valid @RequestBody List<StockChange> list);
}
