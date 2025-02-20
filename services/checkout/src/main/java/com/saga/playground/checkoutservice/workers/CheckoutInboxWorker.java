package com.saga.playground.checkoutservice.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Qualifier("kafkaMessageObjectMapper")
public class CheckoutInboxWorker {

    private final TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    private final ObjectMapper mapper;

    public void bulkSaveMessages(List<Message<byte[]>> listMessages) {
        List<TransactionalInboxOrder> orders = new ArrayList<>();
        for (var msg : listMessages) {
            // get data from json to order
            // actually we can just bulk save all received messages here
            // but when we process them, we need to validate again if it's an already-processed order
            // Therefore, we also extract orderId here and use it for de-duplication
            List<String> extractedData = extractPayloadFromMessage(msg);
            if (extractedData.isEmpty()) {
                continue;
            }

            // push order to list
            orders.add(new TransactionalInboxOrder(
                    extractedData.get(0), extractedData.get(1)));
        }

        try {
            // save all
            transactionalInboxOrderRepository.saveAllAndFlush(orders);
            log.info("INBOX_ORDER_BULK_SAVED {}",
                    orders.stream().map(TransactionalInboxOrder::getOrderId));
        } catch (Exception e) {
            // can't bulk insert -> switch to sequentially insert & log error for manually retry
            sequentialSaveOrders(orders);
        }
    }

    public void sequentialSaveOrders(List<TransactionalInboxOrder> orders) {
        for (var order : orders) {
            try {
                transactionalInboxOrderRepository.save(order);
                log.info("INBOX_ORDER_SAVED {}", order.getOrderId());
            } catch (DataIntegrityViolationException e) {
                log.info("INBOX_ORDER_SQL_ERROR {}", order.getOrderId());
            } catch (Exception e) {
                log.error("INBOX_ORDER_SAVE_ERROR {}", order, e);
            }
        }
    }

    /**
     * @param message The received message from kafka
     * @return List of 2 strings
     * the first element is the string format of the id
     * the second element is the raw converted payload of message (in JSON string format)
     */
    public List<String> extractPayloadFromMessage(Message<byte[]> message) {

        // get string payload from byte[]
        String rawPayload = new String(message.getPayload(), StandardCharsets.US_ASCII);
        log.info("INBOX_ORDER_EXTRACTED_MSG: {}", rawPayload);

        try {
            // parse json from string
            var payload = mapper.readValue(rawPayload, KafkaCreatedOrderMessage.class);
            return List.of(String.valueOf(payload.payload().after().id()), rawPayload);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse payload from message {}", rawPayload, e);
        } catch (Exception e) {
            log.error("UNHANDLED_ERROR Failed to parse payload from message {}", rawPayload, e);
        }

        return Collections.emptyList();
    }

}
