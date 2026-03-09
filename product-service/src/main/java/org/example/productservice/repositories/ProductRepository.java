package org.example.productservice.repositories;

import org.example.productservice.dto.ProductDto;
import org.example.productservice.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> getProductBySku(UUID sku);
}
