package com.saga.playground.checkoutservice.utils.http.error;

import com.saga.playground.checkoutservice.constants.ErrorConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CommonHttpErrorTest {
    @Test
    void verifyError() {
        HttpError err = CommonHttpError.ILLEGAL_ARGS;
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, err.getHttpStatus(),
                "Status code should be equal");
        Assertions.assertEquals(ErrorConstant.CODE_ILLEGAL_ARG, err.getCode(),
                "Code should be equal");
    }
}