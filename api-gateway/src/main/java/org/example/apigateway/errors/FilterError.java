package org.example.apigateway.errors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
public class FilterError extends RuntimeException{

    private String message;
    private String code;
    private final HttpStatus status = HttpStatus.BAD_REQUEST;

    public FilterError(String message, String code) {
        super();
        this.message = message;
        this.code = code;
    }
}
