package org.example.userservice.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class UserExcpetion extends RuntimeException {
    private final HttpStatus status = HttpStatus.BAD_REQUEST;
    private String code;
    public UserExcpetion(String message, String code) {
        super();
    }
}
