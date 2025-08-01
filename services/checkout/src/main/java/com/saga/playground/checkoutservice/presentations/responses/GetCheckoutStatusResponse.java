package com.saga.playground.checkoutservice.presentations.responses;

// to keep it short only orderId is used
// for actual response schema, please check with your PG
// e.g:
// - https://developer.paypal.com/api/nvp-soap/ipn/IPNIntro/#a-sample-ipn-message-and-response
//

import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;

public record GetCheckoutStatusResponse(
    String orderId,
    PaymentStatus status,
    boolean isTerminated // indicate that this is the last state of the checkout, no need for further polling
) {
}
