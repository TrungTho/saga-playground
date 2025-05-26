package com.saga.playground.checkoutservice.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
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

        // Thread.sleep(WorkerConstant.WORKER_CHECKOUT_DELAY_MILLISECONDS);

        // decrypt & validate response (skip)

        persistIPNResponse(response);
    }

    // in actual implementation, sessionId can be used instead of orderId
    public IPNResponse stimulateCallingPaymentGateway(String orderId) {
        log.info("Calling Payment gateway for order {}", orderId);

        return new IPNResponse(orderId, PaymentStatus.FINALIZED);
    }

    public IPNResponse decryptIPNResponse(IPNResponse response) {
        return response;
    }

    /**
     * validate for:
     * - message integrity: checksum/hash/token/encryptedKey/etc.
     * - message duplication: PG send multiple message
     * - sender integrity: header values/IP/etc, (but it should be in infra set up)
     *
     * @param response - response from PG
     * @return boolean true if response is valid and ready to be persisted in DB
     */
    public boolean validateIPNResponse(IPNResponse response) {
        return true;
    }

    @SneakyThrows
    public void persistIPNResponse(IPNResponse response) {
        var checkoutRecord = checkoutRepository.findByOrderId(response.orderId())
            .orElseThrow(() -> {
                    log.info("This is all checkout I can get: {}",
                        checkoutRepository.findAll().stream().map(Checkout::getOrderId).toList());
                    log.info("Can't find checkout record for {}", response);
                    return new HttpException(CommonHttpError.NOT_FOUND_ERROR);
                }
            );

        var jsonRes = objectMapper.writeValueAsString(response);

        checkoutRecord.setCheckoutStatus(response.status());
        checkoutRecord.setWebhookPayload(jsonRes);

        checkoutRepository.save(checkoutRecord);
    }

}
