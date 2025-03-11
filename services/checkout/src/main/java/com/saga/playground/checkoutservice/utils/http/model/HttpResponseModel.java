package com.saga.playground.checkoutservice.utils.http.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class HttpResponseModel<T> {

    private static final String SUCCESS_CODE = "OK";

    private String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;


    /**
     * 200
     *
     * @param data Data
     * @param <T>  Type
     * @return HTTP Response Model
     */
    public static <T> HttpResponseModel<T> success(T data) {
        return HttpResponseModel.<T>builder()
            .code(SUCCESS_CODE)
            .data(data)
            .build();
    }

    /**
     * 4xx & 5xx
     *
     * @param code    Error CodeConstants
     * @param message Message
     * @param <T>     Type
     * @return HTTP Response model
     */
    public static <T> HttpResponseModel<T> error(String code, String message) {
        return HttpResponseModel.<T>builder()
            .code(code)
            .message(message)
            .build();
    }

}
