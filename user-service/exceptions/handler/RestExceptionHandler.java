package org.example.orderservice.exceptions.handler;

import org.example.orderservice.exceptions.ErrorRes;
import org.example.orderservice.exceptions.OrderException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(OrderException.class)
    public final ResponseEntity<ErrorRes> exceptionTokenHandler(Exception e){

        ErrorRes err = new ErrorRes();

        err.setStatus(((OrderException)e).getStatus());
        err.setCode(((OrderException)e).getCode());
        err.setMessage(((OrderException)e).getMessage());


        return ResponseEntity.status(err.getStatus()).body(err);
    }

}
