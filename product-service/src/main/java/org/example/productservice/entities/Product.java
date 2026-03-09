package org.example.productservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    // Identificativo prodotto DB
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Product name cannot be blank")
    @Length(min = 3, message = "Product name must have at least 3 chars")
    private String name;

    // Codice prodotto
    @Column(unique = true)
    private UUID sku;

    // Prezzo, 4 cifre intere, 2 decimali
    @Digits(integer = 4, fraction = 2, message = "Prezzo non valido")
    @DecimalMin(value = "0.0", message = "Il prezzo deve essere positivo")
    private BigDecimal price;

    @Column(nullable = true, columnDefinition = "TEXT")
    @Size(max = 500, message = "Descrizion troppo lunga")
    private String descriptione;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created_at;
}
