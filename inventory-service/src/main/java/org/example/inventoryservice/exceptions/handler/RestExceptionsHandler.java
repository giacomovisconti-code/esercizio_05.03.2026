package org.example.inventoryservice.exceptions.handler;

import org.example.inventoryservice.exceptions.InventoryException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class RestExceptionsHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InventoryException.class)
    public final ResponseEntity<ErrorRes> exceptionTokenHandler(Exception e){

        ErrorRes err = new ErrorRes();

        err.setStatus(((InventoryException)e).getStatus());
        err.setCode(((InventoryException)e).getCode());
        err.setMessage(((InventoryException)e).getMessage());


        return ResponseEntity.status(err.getStatus()).body(err);
    }

}
