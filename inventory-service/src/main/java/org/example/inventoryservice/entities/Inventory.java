package org.example.inventoryservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Codice prodotto
    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Min(value = 0, message = "La quantità deve essere  positiva.")
    private Long quantity;

    @UpdateTimestamp
    private LocalDateTime updatedAt;



}
