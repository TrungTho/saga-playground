package com.saga.playground.checkoutservice.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.presentations.responses.IPNResponse;
import com.saga.playground.checkoutservice.utils.http.error.CommonHttpError;
import com.saga.playground.checkoutservice.utils.http.error.HttpException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This class will be used to simulate the Payment Gateway
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayHandler {

    private final CheckoutRepository checkoutRepository;

    private final ObjectMapper objectMapper;

    /**
     * ref: <a href="https://developer.paypal.com/api/nvp-soap/ipn/">...</a>
     * In actual server, Payment Gateway (PG) will send back to BE
     * the IPN when user finalized the payment with the PG
     * <p>
     * We don't have the real PG in this playground yet.
     * Therefore, this class will be used to simulate that behavior
     */
    public void simulateWebhookReceived(IPNResponse response) {
        log.info("Received IPN for order {}", response.orderId());

        // decrypt & validate response (skip)

        persistIPNResponse(response);
    }

    public IPNResponse decryptIPNResponse(IPNResponse response) {
        return response;
    }

    public boolean validateIPNResponse(IPNResponse response) {
        return true;
    }

    @SneakyThrows
    public void persistIPNResponse(IPNResponse response) {
        var checkoutRecord = checkoutRepository.findByOrderId(response.orderId())
            .orElseThrow(() -> new HttpException(CommonHttpError.NOT_FOUND_ERROR));

        var jsonRes = objectMapper.writeValueAsString(response);

        checkoutRecord.setWebhookPayload(jsonRes);
        checkoutRecord.setCheckoutStatus(response.status());

        checkoutRepository.save(checkoutRecord);
    }

}
