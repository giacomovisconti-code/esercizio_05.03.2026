package org.example.userservice.exceptions.handler;

import org.example.userservice.exceptions.AuthenticationException;
import org.example.userservice.exceptions.ErrorRes;
import org.example.userservice.exceptions.UserExcpetion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(UserExcpetion.class)
    public final ResponseEntity<ErrorRes> exceptionUserHandler(Exception e){

        ErrorRes err = new ErrorRes();

        err.setStatus(((UserExcpetion)e).getStatus());
        err.setCode(((UserExcpetion)e).getCode());
        err.setMessage(((UserExcpetion)e).getMessage());


        return ResponseEntity.status(err.getStatus()).body(err);
    }

    @ExceptionHandler(AuthenticationException.class)
    public final ResponseEntity<ErrorRes> exceptionAuthHandler(Exception e){

        ErrorRes err = new ErrorRes();

        err.setStatus(((AuthenticationException)e).getStatus());
        err.setCode(((AuthenticationException)e).getCode());
        err.setMessage(((AuthenticationException)e).getMessage());


        return ResponseEntity.status(err.getStatus()).body(err);
    }

}
