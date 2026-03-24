package org.example.apigateway.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class TokenError extends RuntimeException{

    private String message;
    private String code;
    private final HttpStatus status = HttpStatus.BAD_REQUEST;

    public TokenError(String message, String code) {
        super();
        this.message = message;
        this.code = code;
    }
}
