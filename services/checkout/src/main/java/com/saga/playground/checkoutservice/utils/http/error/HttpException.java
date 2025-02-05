package com.saga.playground.checkoutservice.utils.http.error;

import lombok.Getter;

@Getter
public class HttpException extends RuntimeException {
    /**
     * Internal Http Error system
     */
    private HttpError error;

    public HttpException(HttpError error) {
        super(error.getMessage());
        this.error = error;
    }
}
