package org.example.orderservice.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class OrderException extends RuntimeException {
    private final HttpStatus status = HttpStatus.BAD_REQUEST;
    private String code;
    public OrderException(String message, String code) {
        super();
    }
}
