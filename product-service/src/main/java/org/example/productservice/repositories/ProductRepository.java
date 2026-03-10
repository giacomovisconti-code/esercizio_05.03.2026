package org.example.productservice.repositories;

import org.example.productservice.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySku(UUID sku);

    Boolean existsBySku(UUID sku);

    void deleteBySku(UUID sku);

    List<Product> findByNameContaining(String name);
}
