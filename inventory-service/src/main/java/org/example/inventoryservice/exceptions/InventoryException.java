package org.example.inventoryservice.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
public class InventoryException extends RuntimeException {
    private final HttpStatus status = HttpStatus.BAD_REQUEST;
    private String code;
    private String message;
    public InventoryException(String code, String message) {
        super();
        this.message = message;
        this.code = code;
    }
}
