package org.example.productservice.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.QueryParam;
import org.example.productservice.dto.ProductDto;
import org.example.productservice.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    //? INDEX
    @GetMapping("/all")
    public ResponseEntity<Page<ProductDto>> getAllProducts(@Parameter(description = "Risultati per pagina", schema = @Schema(defaultValue = "9", minimum = "1"))
                                                               @RequestParam(name = "pageSize", required = false, defaultValue = "9") @Positive(message = "Deve essere >= 1") int pageSize,
                                                           @Parameter(description = "Indice della pagina corrente", schema = @Schema(defaultValue = "0", minimum = "0"))
                                                               @RequestParam(name = "page", required = false, defaultValue = "0") @Min(value = 0, message = "Indice di pagina deve essere >= 0") int page){
        return ResponseEntity.ok(productService.findAllProducts(pageSize, page));
    }

    //? SHOW
    @GetMapping("/{sku}")
    public ResponseEntity<ProductDto> getProduct(@NotNull @PathVariable("sku") UUID sku){
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    //? Ricerca per nome
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDto>> getProductsByName(@NotBlank @RequestParam("name") String name,
                                                              @Parameter(description = "Quante pagine contiene", schema = @Schema(defaultValue = "9", minimum = "1"))
                                                              @RequestParam(name = "pageSize", required = false, defaultValue = "9") @Positive(message = "Deve essere >= 1") int pageSize,
                                                              @Parameter(description = "Indice della pagina corrente", schema = @Schema(defaultValue = "0", minimum = "0"))
                                                              @RequestParam(name = "page", required = false, defaultValue = "0") @Min(value = 0, message = "Indice di pagina deve essere >= 0") int page){
        return ResponseEntity.ok(productService.getProductsByName(name, pageSize, page));
    }

    //? CREATE
    @PostMapping("/create")
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductDto productDto) throws ResponseStatusException {
        productService.createProduct(productDto);
        return ResponseEntity.ok("Prodotto creato con successo");
    }

    //? UPDATE
    @PatchMapping("/update")
    public ResponseEntity<?> updateProduct(@Valid @RequestBody ProductDto productDto) throws ResponseStatusException {
        productService.updateProduct(productDto);
        return ResponseEntity.ok("Prodotto aggiornato con successo");
    }

    //? DELETE
    @DeleteMapping("/delete/{sku}")
    public ResponseEntity<?> deleteProduct(@NotNull @PathVariable("sku") UUID sku){
        productService.deleteProduct(sku);
        return ResponseEntity.ok("Prodotto eliminato con successo");
    }
}
