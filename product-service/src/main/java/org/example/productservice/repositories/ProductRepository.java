package org.example.productservice.repositories;

import org.example.productservice.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySku(UUID sku);

    Page<Product> findAll(Pageable pageable);

    Boolean existsBySku(UUID sku);

    void deleteBySku(UUID sku);

    Page<Product> findByNameContaining(String name, Pageable pageable);
}
