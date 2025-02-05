package com.saga.playground.checkoutservice.utils.http.error;

import org.springframework.http.HttpStatus;

/**
 * Interface for defining Http Error across domains
 */
public interface HttpError {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
