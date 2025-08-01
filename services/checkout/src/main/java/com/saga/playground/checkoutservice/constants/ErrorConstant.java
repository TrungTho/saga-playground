package com.saga.playground.checkoutservice.constants;

public final class ErrorConstant {
    public static final String CODE_ILLEGAL_ARG = "ILLEGAL_ARG";
    public static final String CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String CODE_UNAUTHORIZED_ERROR = "UNAUTHORIZED_ERROR";
    public static final String CODE_NOT_FOUND_ERROR = "NOT_FOUND_ERROR";
    public static final String CODE_TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
    public static final String CODE_UNHANDED_ERROR = "UNHANDED_ERROR";

    public static final String CODE_RETRY_LIMIT_EXCEEDED = "RETRY_LIMIT_EXCEEDED";
    public static final String CODE_TIMEOUT = "TIMEOUT";

    private ErrorConstant() {
    }
}
