package org.example.productservice.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private UUID id;

    private UUID sku;

    @NotBlank(message = "Product name cannot be blank")
    @Length(min = 3, message = "Product name must have at least 3 chars")
    private String name;

    @Size(max = 500, message = "Too long description")
    private String description;

    @Digits(integer = 6, fraction = 2, message = "Not valid Price (max 6 integer digits and 2 decimal places)")
    @DecimalMin(value = "0.01", message = "Price must be positive, at least 0.01")
    private BigDecimal price;

}
