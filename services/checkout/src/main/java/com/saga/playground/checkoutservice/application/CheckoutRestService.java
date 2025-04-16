package com.saga.playground.checkoutservice.application;


import com.saga.playground.checkoutservice.presentations.responses.GetCheckoutStatusResponse;

public interface CheckoutRestService {
    GetCheckoutStatusResponse getCheckoutStatus(String orderId);
}
