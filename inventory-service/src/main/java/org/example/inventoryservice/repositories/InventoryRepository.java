package org.example.inventoryservice.repositories;

import org.example.inventoryservice.entities.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findBySku(UUID sku);

    void deleteBySku(UUID sku);
}
