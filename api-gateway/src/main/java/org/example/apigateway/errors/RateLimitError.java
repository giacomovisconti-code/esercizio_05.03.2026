package org.example.apigateway.errors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
public class RateLimitError extends RuntimeException {
  private String message;
  private String code;
  private final HttpStatus status = HttpStatus.BAD_REQUEST;

  public RateLimitError(String message, String code) {
    super();
    this.message = message;
    this.code = code;
  }
}

