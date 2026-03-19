package org.example.orderservice.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDraft {

    @NotNull
    private UUID sku;

    @Min(value = 1)
    private Long quantity;

    @Digits(integer = 6, fraction = 2, message = "Prezzo non valido (max 6 cifre intere e 2 decimali)")
    @DecimalMin(value = "0.0", message = "Il prezzo deve essere positivo")
    @NotNull
    private BigDecimal price;

}
