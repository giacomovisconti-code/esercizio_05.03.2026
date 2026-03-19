package org.example.orderservice.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemToOrder {

    @NotNull
    private UUID sku;

    @Min(value = 1)
    private Long quantity;

}
