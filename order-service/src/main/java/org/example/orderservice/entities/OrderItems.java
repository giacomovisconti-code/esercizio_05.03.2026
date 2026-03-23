package org.example.orderservice.entities;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Fetch;
import jakarta.validation.Constraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"order_id", "sku"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor

public class OrderItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku")
    private UUID sku;

    // Imposto il limite minimo a 1
    @Min(value = 1)
    private Long quantity;

    @Digits(integer = 6, fraction = 2, message = "Prezzo non valido (max 6 cifre intere e 2 decimali)")
    @DecimalMin(value = "0.0", message = "Il prezzo deve essere positivo")
    private BigDecimal unitPrice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

}
