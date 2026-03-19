package org.example.orderservice.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockRequest {

        private UUID sku;

        @Min(value = 0)
        private Long quantity;

        private LocalDateTime updatedAt;

}
