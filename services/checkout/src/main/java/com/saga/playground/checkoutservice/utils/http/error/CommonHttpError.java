package com.saga.playground.checkoutservice.utils.http.error;

import com.saga.playground.checkoutservice.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonHttpError implements HttpError {
    ILLEGAL_ARGS(
            HttpStatus.BAD_REQUEST,
            ErrorConstant.CODE_ILLEGAL_ARG,
            "Illegal arguments specified for the request."
    ),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorConstant.CODE_INTERNAL_SERVER_ERROR,
            "Something went wrong from the server side."
    ),
    NOT_FOUND_ERROR(
            HttpStatus.NOT_FOUND,
            ErrorConstant.CODE_NOT_FOUND_ERROR,
            "Not found."
    ),
    UNAUTHORIZED_ERROR(
            HttpStatus.UNAUTHORIZED,
            ErrorConstant.CODE_UNAUTHORIZED_ERROR,
            "Unauthorized."
    ),

    TOO_MANY_REQUESTS(
            HttpStatus.TOO_MANY_REQUESTS,
            ErrorConstant.CODE_TOO_MANY_REQUESTS,
            "Too many requests, please retry after some time."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
