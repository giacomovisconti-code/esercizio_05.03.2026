package org.example.productservice.exceptions.handler;

import org.example.productservice.exceptions.ProductException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(ProductException.class)
    public final ResponseEntity<ErrorRes> exceptionTokenHandler(Exception e){

        ErrorRes err = new ErrorRes();

        err.setStatus(((ProductException)e).getStatus());
        err.setCode(((ProductException)e).getCode());
        err.setMessage(((ProductException)e).getMessage());


        return ResponseEntity.status(err.getStatus()).body(err);
    }

}
