package org.example.userservice.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserExcpetion extends RuntimeException {
    private final HttpStatus status = HttpStatus.BAD_REQUEST;
    private String code;
    private String message;
    public UserExcpetion(String code, String message) {

        super();
        this.code = code;
        this.message = message;
    }
}
