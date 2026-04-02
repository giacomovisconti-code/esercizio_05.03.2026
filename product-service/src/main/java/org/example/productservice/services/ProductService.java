package org.example.productservice.services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import org.example.productservice.dto.ProductDto;
import org.example.productservice.entities.Product;
import org.example.productservice.exceptions.Errors;
import org.example.productservice.exceptions.ProductException;
import org.example.productservice.openfeign.InventoryClient;
import org.example.productservice.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private InventoryClient inventoryClient;


    private final ModelMapper modelMapper = new ModelMapper();

    //? UTILS
    // PRODOTTO: entità --> Dto
    private ProductDto convertToDto(Product p){
        return modelMapper.map(p, ProductDto.class);
    }
    // PRODOTTO: DTO --> entità
    private Product convertToEntity(ProductDto p){
        return modelMapper.map(p, Product.class);
    }

    // CREAZIONE GIACENZA
    @CircuitBreaker(name = "default", fallbackMethod = "fallBack")
    private void createStock(UUID sku) {
        inventoryClient.createStock(sku);
    }

    // METODO FALLBACK PER IL CIRCUIT BREAKER INVNTORY SERVICE
    private void fallBack() {
        throw new ProductException(Errors.STOCK_NOT_CREATED.key(), Errors.STOCK_NOT_CREATED.message());
    }


    // Validazione del Dto in entrata
    private void productValidation(ProductDto p) throws ResponseStatusException {
        if (p.getPrice() == null){
            throw new ProductException(Errors.INVALID_PRODUCT_PRICE.key(), Errors.INVALID_PRODUCT_PRICE.message());
        }
        if (p.getPrice().min(BigDecimal.ZERO).equals(p.getPrice())){
            throw new ProductException(Errors.INVALID_PRODUCT_PRICE.key(), Errors.INVALID_PRODUCT_PRICE.message());
        }

    }

    //! INDEX
    public Page<ProductDto> findAllProducts(int pageSize, int page){
        //Creo la paginazione per
        Pageable pageable = PageRequest.of(page, pageSize);

        // Salvo la lista di prodotti
        Page<Product> productList = productRepository.findAll(pageable);

        // Converto in Product DTO
        return productList.map(this::convertToDto);

    }

    //! SHOW
    @Cacheable(value = "product", key = "#sku")
    public ProductDto getProductBySku(UUID sku){
    Product p = productRepository.findBySku(sku).orElseThrow(() -> new ProductException(Errors.PRODUCT_NOT_FOUND.key(), Errors.PRODUCT_NOT_FOUND.message()));

        return convertToDto(p);
    }

    //! Ricerca prodotto per nome
    public Page<ProductDto> getProductsByName(String name, int pageSize, int page){
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Product> productList = productRepository.findByNameContaining(name, pageable);

        return productList.map(this::convertToDto);
    }

    //! CREATE
    @Transactional
    public void createProduct(ProductDto productRequest) throws ResponseStatusException {
        if (productRepository.existsBySku(productRequest.getSku())){
            throw new ProductException(Errors.PRODUCT_ALREADY_REGISTERED.key(), Errors.PRODUCT_ALREADY_REGISTERED.message());
        }

        productValidation(productRequest);

        Product p = convertToEntity(productRequest);

        productRepository.save(p);

        // Elimino il dato dalla cache
        Objects.requireNonNull(cacheManager.getCache("product")).evictIfPresent(productRequest.getSku());

        // Inizializzo la giacenza del prodotto a zero
        createStock(p.getSku());
    }

    //! UPDATE
    public void updateProduct(ProductDto productRequest) throws ResponseStatusException {
        Product p = productRepository.findBySku(productRequest.getSku()).orElseThrow(() -> new ProductException(Errors.PRODUCT_NOT_FOUND.key(), Errors.PRODUCT_NOT_FOUND.message()));

        // Faccio la validazione sul dto per la richiesta di aggiornamento
        productValidation(productRequest);

        // Aggiorno il prodotto
        p.setName(productRequest.getName());
        p.setPrice(productRequest.getPrice());
        p.setDescription(productRequest.getDescription());

        productRepository.save(p);

        // Elimino il dato dalla cache
        Objects.requireNonNull(cacheManager.getCache("product")).evictIfPresent(productRequest.getSku());
    }

    //! DELETE
    @Transactional
    public void deleteProduct(UUID sku){
        productRepository.findBySku(sku).orElseThrow(() -> new ProductException(Errors.PRODUCT_NOT_FOUND.key(), Errors.PRODUCT_NOT_FOUND.message()));

        // Se presente lo elimino
        productRepository.deleteBySku(sku);

        //Elimino anche la giacenza
        inventoryClient.deleteStock(sku);

        // Elimino il dato dalla cache
        Objects.requireNonNull(cacheManager.getCache("product")).evictIfPresent(sku);

    }

}
