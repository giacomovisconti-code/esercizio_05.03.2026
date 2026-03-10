package org.example.productservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @NotBlank(message = "Product name cannot be blank")
    @Length(min = 3, message = "Product name must have at least 3 chars")
    private String name;

    // Codice prodotto
    @Column(unique = true, columnDefinition = "VARCHAR(36)")
    private UUID sku;

    // Prezzo positivo, 6 cifre intere, 2 decimali
    @Digits(integer = 6, fraction = 2, message = "Prezzo non valido (max 6 cifre intere e 2 decimali)")
    @DecimalMin(value = "0.0", message = "Il prezzo deve essere positivo")
    private BigDecimal price;

    @Column(nullable = true, columnDefinition = "TEXT")
    @Size(max = 500, message = "Descrizion troppo lunga")
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created_at;

    @UpdateTimestamp
    private LocalDateTime updated_at;

}
