package org.example.productservice.services;

import org.example.productservice.dto.ProductDto;
import org.example.productservice.entities.Product;
import org.example.productservice.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;


    private ModelMapper modelMapper = new ModelMapper();

    // UTILS
    // PRODOTTO: entità --> Dto
    private ProductDto convertToDto(Product p){
        return modelMapper.map(p, ProductDto.class);
    }
    // PRODOTTO: DTO --> entità
    private Product convertToEntity(ProductDto p){
        return modelMapper.map(p, Product.class);
    }

    // INDEX
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

    // SHOW
    public ProductDto getProductBySku(UUID sku){
        Optional<Product> pOpt = productRepository.getProductBySku(sku);

        // Controllo se presente il prodotto
        if (pOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato!");
        }

        Product p = pOpt.get();

        return convertToDto(p);
    }

    public void createProduct(ProductDto productRequest){

    }



}
