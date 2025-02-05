package com.saga.playground.checkoutservice.utils.http.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonHttpError implements HttpError {
    ILLEGAL_ARGS(
            HttpStatus.BAD_REQUEST,
            "ILLEGAL_ARG",
            "Illegal arguments specified for the request."
    ),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "Something went wrong from the server side."
    ),
    NOT_FOUND_ERROR(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND_ERROR",
            "Not found."
    ),
    UNAUTHORIZED_ERROR(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED_ERROR",
            "Unauthorized."
    ),

    TOO_MANY_REQUESTS(
            HttpStatus.TOO_MANY_REQUESTS,
            "TOO_MANY_REQUESTS",
            "Too many requests, please retry after some time."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
