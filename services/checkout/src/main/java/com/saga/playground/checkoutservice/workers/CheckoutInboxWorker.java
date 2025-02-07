package com.saga.playground.checkoutservice.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutInboxWorker {

    private final TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    public void saveMessages(List<Message<byte[]>> listMessages) {
        try {
            List<TransactionalInboxOrder> orders = new ArrayList<>();
            for (var msg : listMessages) {
                // get data from json to order
                var createdOrder = extractPayloadFromMessage(msg);
                if (createdOrder == null) {
                    continue;
                }

                orders.add(new TransactionalInboxOrder());
                // push order to list

            }

            // save all
            transactionalInboxOrderRepository.saveAllAndFlush(orders);
        } catch (Exception e) {

        }
    }

    public CreatedOrder extractPayloadFromMessage(Message<byte[]> message) {
        ObjectMapper mapper = new ObjectMapper();
        // get string payload from byte[]
        String rawPayload = new String(message.getPayload(), StandardCharsets.US_ASCII);

        try {
            // parse json from string
            return mapper.readValue(rawPayload, CreatedOrder.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse payload from message {}", rawPayload);
        } catch (Exception e) {
            log.error("UNHANDLED_ERROR Failed to parse payload from message {}", rawPayload);
        }

        return null;
    }

    public class CreatedOrder {
        // TODO: body here
    }
}
