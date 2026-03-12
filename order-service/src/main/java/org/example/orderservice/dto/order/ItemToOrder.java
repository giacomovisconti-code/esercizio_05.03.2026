package org.example.orderservice.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemToOrder {

    private UUID sku;
    private Long quantity;

}
