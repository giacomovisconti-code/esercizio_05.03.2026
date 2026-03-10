package org.example.productservice.services;

import jakarta.transaction.Transactional;
import org.example.productservice.dto.ProductDto;
import org.example.productservice.entities.Product;
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

    // Validazione del Dto in entrata
    private void productValidation(ProductDto p) throws ResponseStatusException {
        if (p.getPrice() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prezzo nullo");
        }
        if (p.getPrice().min(new BigDecimal(0)).equals(p.getPrice()) ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prezzo minore di 0");
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
        Optional<Product> pOpt = productRepository.findBySku(sku);

        // Controllo se presente il prodotto
        if (pOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato!");
        }

        Product p = pOpt.get();

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
    public void createProduct(ProductDto productRequest) throws ResponseStatusException {
        if (productRepository.existsBySku(productRequest.getSku())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prodotto già registrato!");
        }

        productValidation(productRequest);

        Product p = convertToEntity(productRequest);

        productRepository.save(p);

    }

    //! UPDATE
    public void updateProduct(ProductDto productRequest) throws ResponseStatusException {
        Optional<Product> pOpt = productRepository.findBySku(productRequest.getSku());

        // Controllo se il prodotto è presente
        if (pOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato!");
        }

        Product p = pOpt.get();

        // Faccio la validazione sul dto per la richiesta di aggiornamento
        productValidation(productRequest);

        // Aggiorno il prodotto
        p.setSku(productRequest.getSku());
        p.setName(productRequest.getName());
        p.setPrice(productRequest.getPrice());
        p.setDescription(productRequest.getDescription());

        productRepository.save(p);
    }

    //! DELETE
    @Transactional
    public void deleteProduct(UUID sku){
        Optional<Product> pOpt = productRepository.findBySku(sku);

        // Controllo se il prodotto è presente
        if (pOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato!");
        }

        // Se presente lo elimino
        productRepository.deleteBySku(sku);

    }





}
