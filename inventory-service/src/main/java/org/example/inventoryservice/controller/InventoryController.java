package org.example.inventoryservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.inventoryservice.dto.StockChange;
import org.example.inventoryservice.dto.StockRequest;
import org.example.inventoryservice.services.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    //! INDEX
    @GetMapping
    public ResponseEntity<List<StockRequest>> getAllStocks(){
        return ResponseEntity.ok(inventoryService.getAllStock());
    }

    //! SHOW
    @GetMapping("/{productId}")
    public ResponseEntity<StockRequest> getStock(@NotNull @PathVariable("productId") UUID productId){
         return  ResponseEntity.ok(inventoryService.getStockByProductId(productId));
    }

    //! CREATE
    // Automatica con OpenFeign
    @PostMapping("/create/{productId}")
    public ResponseEntity<String> createStock(@NotNull @PathVariable("productId") UUID productId) {
        inventoryService.initializeStock(productId);
        return ResponseEntity.ok("Giacenza inizializzata");
    }

    //! DEDUCTION
    // deduction stock
    @PutMapping("/deduction")
    @Validated
    public ResponseEntity<String> deductionStock(@Valid @RequestBody List<StockChange> list){
        inventoryService.deductStock(list);
        return ResponseEntity.ok("Giacenza ridotta con successo");
    }

    //! ADDITION
    // Addition stock
    @PutMapping("/addition")
    @Validated
    public ResponseEntity<String> additionStock(@Valid @RequestBody List<StockChange> list){
        inventoryService.addStock(list);
        return ResponseEntity.ok("Giacenza incrementata con successo");
    }

    //! UPDATE
    @PatchMapping("/update")
    public ResponseEntity<?> updateStock(@Valid @RequestBody StockChange stockChange) {
        inventoryService.modifyStock(stockChange);
        return ResponseEntity.ok("Giacenza modificata correttamente");
    }

    //! DELETE
    // Automatica con OpenFeign
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<String> deleteStock(@PathVariable("productId") UUID productId){
        inventoryService.deleteStock(productId);
       return ResponseEntity.ok("Giacenza eliminata con successo");
    }
}
