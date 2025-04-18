package com.saga.playground.checkoutservice.utils.http.handler.exception;

import com.saga.playground.checkoutservice.utils.http.error.CommonHttpError;
import com.saga.playground.checkoutservice.utils.http.error.HttpError;
import com.saga.playground.checkoutservice.utils.http.error.HttpException;
import com.saga.playground.checkoutservice.utils.http.model.HttpResponseModel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnknownServerError(Exception ex, WebRequest request) {
        clearUncommittedResponseBuffer(request);
        log.error("Internal Server Error occurred: ", ex);
        HttpError err = CommonHttpError.INTERNAL_SERVER_ERROR;
        HttpResponseModel<?> errorModel = HttpResponseModel.error(err.getCode(), err.getMessage());
        return ResponseEntity.status(err.getHttpStatus()).body(errorModel);
    }

    @ExceptionHandler(HttpException.class)
    protected ResponseEntity<Object> handleHTTPException(HttpException ex, WebRequest request) {
        clearUncommittedResponseBuffer(request);
        HttpResponseModel<?> errorModel = HttpResponseModel.error(ex.getError().getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getError().getHttpStatus()).body(errorModel);
    }

    /**
     * Clear uncommitted response buffer to fully overwrite response with error message
     *
     * @param request Web Request
     */
    private void clearUncommittedResponseBuffer(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletResponse response = servletWebRequest.getResponse();
            if (response != null) {
                response.resetBuffer();
            }
        }
    }

}
