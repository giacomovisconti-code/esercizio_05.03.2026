package org.example.inventoryservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;
import org.example.inventoryservice.dto.StockChange;
import org.example.inventoryservice.dto.StockRequest;
import org.example.inventoryservice.servicies.InventoryService;
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
    public ResponseEntity<String> createStock(@NotNull @PathVariable("productId") UUID productId) throws Exception {
        inventoryService.initializeStock(productId);
        return ResponseEntity.ok("Giacenza inizializzata");
    }

    //! DEDUCTION
    // deduction stock
    @PatchMapping("/deduction/{productId}")
    @Validated
    public ResponseEntity<String> deductionStock(@PathVariable("productId") UUID productId,@NotNull @Min(1) @RequestParam("quantity") Long quantity){
        inventoryService.deductStock(productId, quantity);
        return ResponseEntity.ok("Giacenza ridotta con successo");
    }

    //! ADDTION
    // Addition stock
    @PatchMapping("/addition/{sku}")
    @Validated
    public ResponseEntity<String> additionStock(@PathVariable("sku") UUID sku,@NotNull @Min(1) @RequestParam("quantity") Long quantity){
        inventoryService.addStock(sku, quantity);
        return ResponseEntity.ok("Giacenza incrementata con successo");
    }

    //! UPDATE
    @PatchMapping("/update")
    public ResponseEntity<?> updateStock(@Valid @RequestBody StockChange stockChange) throws Exception {
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
