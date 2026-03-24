package org.example.orderservice.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class OrderException extends RuntimeException {
    private final HttpStatus status = HttpStatus.BAD_REQUEST;
    private String code;
    private String message;
    public OrderException(String code, String message) {
        super();
        this.code = code;
        this.message = message;
    }
}
