package org.example.userservice.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class AuthenticationException extends RuntimeException {
    private final HttpStatus status = HttpStatus.BAD_REQUEST;
    private String code;
    public AuthenticationException(String message, String code) {
        super();
    }
}
