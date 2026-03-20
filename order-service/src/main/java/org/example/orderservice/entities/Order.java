package org.example.orderservice.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.orderservice.enums.OrderStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "The user id, must be popolized")
    private UUID userId;

    @Digits(integer = 6, fraction = 2, message = "Non valido (max 6 cifre intere e 2 decimali)")
    @DecimalMin(value = "0.0", message = "Il totale deve essere positivo")
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.BOZZA;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @NotNull
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    List<OrderItems> orderItems;

    @NotNull
    private Boolean active;

    @NotNull
    private Boolean deleted = false;
}
