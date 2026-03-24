package org.example.orderservice.repositories;

import org.example.orderservice.entities.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findOrderById(UUID orderId);
    Page<Order> findAllByDeletedFalse(Pageable pageable);
    Optional<Order> findByIdAndDeletedFalse(UUID orderId);
}
