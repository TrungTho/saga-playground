package com.saga.playground.checkoutservice.workers.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.constants.MessageBrokerConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.events.checkout.CheckoutRegisteredEvent;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.presentations.requests.KafkaCreatedOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutHelper {

    private final ObjectMapper objectMapper;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final CheckoutRepository checkoutRepository;

    /**
     * In order for a checkout which is ready to be processed,
     * it would need to be in INIT/PROCESSING state
     * In short, it's not in terminal states
     *
     * @param status the status of checkout that we want to validate
     * @return true if the entity is valid to be picked up
     */
    public static boolean isTerminalState(PaymentStatus status) {
        return status.equals(PaymentStatus.FINALIZED)
            || status.equals(PaymentStatus.FAILED);
    }

    /**
     * This method will help to build the checkout entity from the transactional inbox order record
     *
     * @param inboxOrder record of the transactional inbox order
     * @return checkout entity which is ready to be persisted
     */
    @SneakyThrows // upstream method will handle exception & retry
    public Optional<Checkout> upsertCheckoutInfo(TransactionalInboxOrder inboxOrder) {
        // check if checkout entity already existed
        var existingRecord = checkoutRepository.findByOrderId(inboxOrder.getOrderId());
        if (existingRecord.isPresent()) {
            // existed -> validate status
            if (!isTerminalState(existingRecord.get().getCheckoutStatus())) {
                return existingRecord;
            } else {
                log.info("INVALID CHECKOUT STATE {}", existingRecord);
                return Optional.empty();
            }
        } else {
            // not existed -> but new entity

            var payload =
                objectMapper.readValue(inboxOrder.getPayload(), KafkaCreatedOrderMessage.class);

            BigDecimal decodedAmount = decodeAmount(payload.payload().after().amount());

            var checkout = new Checkout(
                "%d".formatted(payload.payload().after().id()),
                payload.payload().after().userId(),
                PaymentStatus.INIT,
                decodedAmount
            );

            checkoutRepository.save(checkout);
            return Optional.of(checkout);
        }
    }

    /**
     * <a href="https://debezium.io/documentation/faq/#how_to_retrieve_decimal_field_from_binary_representation">...</a>
     * We are using Debezium to capture the changes from order service, the amount is of type decimal in postgres
     * That value will be encoded by Debezium before publishing to Kafka
     * We need to decode it to get the actual decimal value here
     *
     * @param encodedAmount encoded value from Debezium
     * @return decodedValue - the original decimal value
     */
    BigDecimal decodeAmount(String encodedAmount) {
        BigInteger integer = new BigInteger(Base64.getDecoder().decode(encodedAmount));
        return new BigDecimal(integer, MessageBrokerConstant.ORDER_AMOUNT_SCALE);
    }

    /**
     * This method will handle to core logic of checkout
     * It will make call to the payment gateway in the actual implementation
     * For demo purpose, it will just stimulate a call here
     *
     * @param checkoutInfo Checkout entity
     * @return sessionId of the checkout from Payment gateway which can be used later
     */

    @SneakyThrows // we will rarely interrupt the method
    public String registerCheckout(Checkout checkoutInfo) {
        try {
            log.info("Call payment gateway for checking out order {}", checkoutInfo.getOrderId());
            Thread.sleep(WorkerConstant.WORKER_CHECKOUT_DELAY_MILLISECONDS);
            return UUID.randomUUID().toString(); // fake a session id from payment gateway
        } catch (Exception e) {
            log.error("Error when call payment gateway for order {}", checkoutInfo.getOrderId(), e);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * This method will be used to process ASYNC post checkout process
     * E.g:
     * - Status updating
     * - Notification sending
     * - Report rendering
     * - etc.
     * For now, we just keep it blank with a log
     *
     * @param checkoutInfo information of the processing checkout
     */
    public void postCheckoutProcess(Checkout checkoutInfo) {
        log.info("POST_CHECKOUT {}", checkoutInfo.getOrderId());
        // more logic will be implemented here for post checkout process

        // simulate the webhook receiver by publishing an async message
        applicationEventPublisher.publishEvent(new CheckoutRegisteredEvent(checkoutInfo.getOrderId()));
    }
}
