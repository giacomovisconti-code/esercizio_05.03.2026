package org.example.orderservice.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDraft {

    private UUID productId;
    private Long quantity;
    private BigDecimal price;

}
