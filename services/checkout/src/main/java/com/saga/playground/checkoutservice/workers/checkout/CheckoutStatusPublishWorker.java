package com.saga.playground.checkoutservice.workers.checkout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.constants.ErrorConstant;
import com.saga.playground.checkoutservice.constants.MessageBrokerConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.presentations.requests.KafkaCheckoutStatusMessage;
import com.saga.playground.checkoutservice.utils.locks.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
@Qualifier("kafkaMessageObjectMapper")
public class CheckoutStatusPublishWorker {

    private final CheckoutRepository checkoutRepository;

    private final DistributedLock distributedLock;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public void publishCheckoutStatus() {
        // acquire distributed lock with timeout
        if (distributedLock.acquireLock(WorkerConstant.WORKER_CHECKOUT_STATUS_PUBLISH_LOCK)) {
            try {
                // query status that are not published
                var checkouts = this.getCheckoutRecordForPublishing();
                publish(checkouts);
            } catch (Exception e) {
                log.error(ErrorConstant.CODE_UNHANDED_ERROR, e);
            } finally {
                distributedLock.releaseLock(WorkerConstant.WORKER_CHECKOUT_STATUS_PUBLISH_LOCK);
            }
        } else {
            // cannot acquire lock -> log and out
            log.info("{} Can't acquire lock to publish checkout status",
                Thread.currentThread().getName());
        }
    }

    public void publish(List<Checkout> checkouts) {
        log.info("Start publishing checkout for {}",
            checkouts.stream().map(Checkout::getOrderId).toList());
        List<CompletableFuture<SendResult<String, String>>> futureList = new ArrayList<>();

        // update db as sent
        checkouts.forEach(checkout -> {
            var message = new KafkaCheckoutStatusMessage(checkout.getOrderId(), checkout.getCheckoutStatus());
            String stringPayload = "";
            try {
                stringPayload = objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                log.error("Can't parse data of checkout record {}", checkout);
            }

            if (Strings.isBlank(stringPayload)) {
                // no payload can be parsed -> skip this entry
                return;
            }

            // send to queue with key = order_id (idempotent)
            var futureRequest = kafkaTemplate.send(
                MessageBrokerConstant.CHECKOUT_STATUS_TOPIC,
                checkout.getOrderId(),
                stringPayload
            ).exceptionally(e ->
                {
                    // ensure that allOf won't infinity throw exception
                    log.error("ERROR_KAFKA_SEND for checkout {}", checkout, e);
                    return null;
                }
            );
            futureList.add(futureRequest);
            checkout.setEventPublished(true);
        });

        // waiting for all message publishing become successful
        CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new)).join();

        checkoutRepository.saveAll(checkouts);
    }

    public List<Checkout> getCheckoutRecordForPublishing() {
        List<Checkout> res = new ArrayList<>();

        WorkerConstant.NOTIFIABLE_CHECKOUT_STATUSES
            .forEach(status -> res.addAll(
                checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(status, false)));

        return res;
    }

}
