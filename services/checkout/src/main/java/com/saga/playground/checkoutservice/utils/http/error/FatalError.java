package com.saga.playground.checkoutservice.utils.http.error;

import lombok.Getter;

/**
 * there is some error that's not relate to http server
 * those can't be handled properly regardless,
 * therefore, we can just throw the error and let app crash
 * further investigation will be needed from developers
 */
@Getter
public class FatalError extends Error {
    public FatalError(String msg) {
        super(msg);
    }
}
