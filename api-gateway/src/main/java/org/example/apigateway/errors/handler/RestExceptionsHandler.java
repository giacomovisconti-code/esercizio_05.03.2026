package org.example.apigateway.errors.handler;

import org.example.apigateway.errors.FilterError;
import org.example.apigateway.errors.RateLimitError;
import org.example.apigateway.errors.TokenError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class RestExceptionsHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TokenError.class)
    public final ResponseEntity<ErrorRes> exceptionTokenHandler(Exception e){

        ErrorRes err = new ErrorRes();

        err.setStatus(((TokenError)e).getStatus());
        err.setCode(((TokenError)e).getCode());
        err.setMessage(((TokenError)e).getMessage());


        return ResponseEntity.status(err.getStatus()).body(err);
    }

    @ExceptionHandler(FilterError.class)
    public final ResponseEntity<ErrorRes> exceptionFilterHandler(Exception e){

        ErrorRes err = new ErrorRes();

        err.setStatus(((FilterError)e).getStatus());
        err.setCode(((FilterError)e).getCode());
        err.setMessage(((FilterError)e).getMessage());

        return ResponseEntity.status(err.getStatus()).body(err);

    }

    @ExceptionHandler(RateLimitError.class)
    public final ResponseEntity<ErrorRes> exceptionRateLimitHandler(Exception e){

        ErrorRes err = new ErrorRes();

        err.setStatus(((RateLimitError)e).getStatus());
        err.setCode(((RateLimitError)e).getCode());
        err.setMessage(((RateLimitError)e).getMessage());

        return ResponseEntity.status(err.getStatus()).body(err);

    }
}
