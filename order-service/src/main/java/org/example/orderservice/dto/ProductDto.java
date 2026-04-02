package org.example.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto {

    private UUID id;

    private UUID sku;

    @NotBlank(message = "Product name cannot be blank")
    @Length(min = 3, message = "Product name must have at least 3 chars")
    private String name;

    @Digits(integer = 6, fraction = 2, message = "Prezzo non valido (max 6 cifre intere e 2 decimali)")
    @DecimalMin(value = "0.0", message = "Il prezzo deve essere positivo")
    private BigDecimal price;

}
