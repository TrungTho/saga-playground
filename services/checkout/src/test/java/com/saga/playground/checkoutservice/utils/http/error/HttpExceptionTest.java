package com.saga.playground.checkoutservice.utils.http.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpExceptionTest {

    @Test
    void testHttpException() {
        assertThrows(HttpException.class, () -> {
            throw new HttpException(CommonHttpError.INTERNAL_SERVER_ERROR);
        });
    }

}
