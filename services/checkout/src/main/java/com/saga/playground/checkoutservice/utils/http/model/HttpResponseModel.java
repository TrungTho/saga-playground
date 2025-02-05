package com.saga.playground.checkoutservice.utils.http.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

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
     * @param data Data
     * @return HTTP Response Model
     * @param <T> Type
     */
    public static <T> HttpResponseModel<T> success(T data) {
        return HttpResponseModel.<T>builder()
                .code(SUCCESS_CODE)
                .data(data)
                .build();
    }

    /**
     * 204
     * @return HTTP Response Model
     * @param <T> Type
     */
    public static <T> HttpResponseModel<T> success() {
        return HttpResponseModel.<T>builder()
                .code(SUCCESS_CODE)
                .build();
    }

    /**
     * 4xx & 5xx
     * @param code Error CodeConstants
     * @param message Message
     * @return HTTP Response model
     * @param <T> Type
     */
    public static <T> HttpResponseModel<T> error(String code, String message) {
        return HttpResponseModel.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

}
