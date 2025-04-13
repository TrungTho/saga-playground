package com.saga.playground.checkoutservice.workers.checkout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.presentations.requests.KafkaCreatedOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutHelper {
    private final ObjectMapper objectMapper;

    /**
     * This method will help to build the checkout entity from the transactional inbox order record
     *
     * @param inboxOrder record of the transactional inbox order
     * @return checkout entity which is ready to be persisted
     * @throws JsonProcessingException if parser can't parse the order inbox record
     */
    public Checkout buildCheckoutInfo(TransactionalInboxOrder inboxOrder) throws JsonProcessingException {
        var payload = objectMapper.readValue(inboxOrder.getPayload(), KafkaCreatedOrderMessage.class);
        // todo parse amount
        return new Checkout(
            "%d".formatted(payload.payload().after().id()),
            payload.payload().after().userId(),
            PaymentStatus.INIT,
            BigDecimal.ONE
        );
    }

    /**
     * This method will handle to core logic of checkout
     * It will make call to the payment gateway in the actual implementation
     * For demo purpose, it will just stimulate a call here
     *
     * @param checkoutInfo Checkout entity
     * @return sessionId of the checkout from Payment gateway which can be used later
     * @throws InterruptedException demo exception
     */

    public String checkout(Checkout checkoutInfo) throws InterruptedException {
        try {
            log.info("Call payment gateway for checking out order {}", checkoutInfo.getOrderId());
            Thread.sleep(1_000);
            return UUID.randomUUID().toString();// fake a session id from payment gateway
        } catch (Exception e) {
            log.error("CHECKOUT PAYMENT GATEWAY order {}", checkoutInfo.getOrderId(), e);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public void postCheckoutProcess(Checkout checkoutInfo) {

    }
}
