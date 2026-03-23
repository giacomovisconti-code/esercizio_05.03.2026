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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryClient inventoryClient;


    private ModelMapper modelMapper = new ModelMapper();

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
    private ProductException fallBack(Exception e) {
        return new ProductException(Errors.STOCK_NOT_CREATED.key(), Errors.STOCK_NOT_CREATED.message());
    }


    // Validazione del Dto in entrata
    private void productValidation(ProductDto p) throws ResponseStatusException {
        if (p.getPrice() == null){
            throw new ProductException(Errors.PRODUCT_NOT_FOUND.key(), Errors.PRODUCT_NOT_FOUND.message());
        }
        if (p.getPrice().min(new BigDecimal(0)).equals(p.getPrice()) ){
            throw new ProductException(Errors.INVALID_PRODUCT_PRICE.key(), Errors.INVALID_PRODUCT_PRICE.message());
        }

    }

    //! INDEX
    public List<ProductDto> findAllProducts(){

        // Salvo la lista di prodotti
        List<Product> productList = productRepository.findAll();

        // Converto in Product DTO
        return productList.stream()
                // Per ogni elemento applica il metodo convertToDto
                .map(this::convertToDto)
                // E ritorna una lista di product dto
                .collect(Collectors.toList());
    }

    //! SHOW
    public ProductDto getProductBySku(UUID sku){
    Product p = productRepository.findBySku(sku).orElseThrow(() -> new ProductException(Errors.PRODUCT_NOT_FOUND.key(), Errors.PRODUCT_NOT_FOUND.message()));

        return convertToDto(p);
    }

    //! Ricerca prodotto per nome
    public List<ProductDto> getProductsByName(String name){
        List<Product> productList = productRepository.findByNameContaining(name);

        return productList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
    }

    //! DELETE
    @Transactional
    public void deleteProduct(UUID sku){
        Product p = productRepository.findBySku(sku).orElseThrow(() -> new ProductException(Errors.PRODUCT_NOT_FOUND.key(), Errors.PRODUCT_NOT_FOUND.message()));

        // Se presente lo elimino
        productRepository.deleteBySku(sku);
        //Elimino anche la giacenza
        inventoryClient.deleteStock(sku);

    }





}
