package com.saga.playground.checkoutservice.application.impl;


import com.saga.playground.checkoutservice.application.CheckoutRestService;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.presentations.responses.GetCheckoutStatusResponse;
import com.saga.playground.checkoutservice.utils.http.error.CommonHttpError;
import com.saga.playground.checkoutservice.utils.http.error.HttpException;
import com.saga.playground.checkoutservice.webhooks.PaymentGatewayHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutRestServiceImpl implements CheckoutRestService {

    private final CheckoutRepository checkoutRepository;

    private final PaymentGatewayHandler paymentGatewayHandler;

    @Override
    public GetCheckoutStatusResponse getCheckoutStatus(String orderId) {
        var checkout = checkoutRepository.findByOrderId(orderId)
            .orElseThrow(() -> new HttpException(CommonHttpError.NOT_FOUND_ERROR));

        // already in terminal state -> no further update from PG -> just return record
        if (isTerminalState(checkout.getCheckoutStatus())) {
            return new GetCheckoutStatusResponse(orderId, checkout.getCheckoutStatus(), true);
        } else {
            // call PG to retrieved checkout status
            var resp = paymentGatewayHandler.stimulateCallingPaymentGateway(orderId);

            // update DB if needed
            if (!checkout.getCheckoutStatus().equals(resp.status())) {
                checkout.setCheckoutStatus(resp.status());

                // we don't need to explicit save because entity manager will help,
                // but we still put it here for conditional unit test verification
                checkoutRepository.save(checkout);
            }

            // return
            return new GetCheckoutStatusResponse(orderId, resp.status(), isTerminalState(resp.status()));
        }
    }

    public boolean isTerminalState(PaymentStatus status) {
        return (status.equals(PaymentStatus.FINALIZED)
            || status.equals(PaymentStatus.FAILED));
    }
}
