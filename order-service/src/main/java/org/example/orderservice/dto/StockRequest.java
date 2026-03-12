package org.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockRequest {
        private UUID productId;
        private Long quantity;
        private LocalDateTime updatedAt;

}
