package com.saga.playground.checkoutservice.kafka;

import com.saga.playground.checkoutservice.constants.ConsumerConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    // configurations

    // consumers registration
    @KafkaListener(
            groupId = ConsumerConstant.ORDER_CREATED_CONSUMER_GROUP_ID,
            topics = ConsumerConstant.ORDER_CREATED_TOPIC,
            concurrency = "1" // for demo purpose,
    )
    public void pullCreatedOrder(List<Message<byte[]>> list, Acknowledgment ack) {
        log.info("INBOX_ORDER_START received from topic {}, offset {} - {}",
                list.get(0).getHeaders().get(KafkaHeaders.RECEIVED_TOPIC),
                list.get(0).getHeaders().get(KafkaHeaders.OFFSET),
                list.get(list.size() - 1).getHeaders().get(KafkaHeaders.OFFSET)
        );
        for (var msg : list) {
            log.info("received: {}", msg);
            String payload = new String(msg.getPayload(), StandardCharsets.US_ASCII);
            log.info("extracted msg: {}", payload);
        }

        // bulk insert to db
        log.info("========================================");

        // batching ack to kafka
        log.info("INBOX_ORDER_ACK to topic {}, offset {} - {}",
                list.get(0).getHeaders().get(KafkaHeaders.RECEIVED_TOPIC),
                list.get(0).getHeaders().get(KafkaHeaders.OFFSET),
                list.get(list.size() - 1).getHeaders().get(KafkaHeaders.OFFSET)
        );
        // ack.acknowledge();

        log.info("INBOX_ORDER_FINISHED topic {}, offset {} - {}",
                list.get(0).getHeaders().get(KafkaHeaders.RECEIVED_TOPIC),
                list.get(0).getHeaders().get(KafkaHeaders.OFFSET),
                list.get(list.size() - 1).getHeaders().get(KafkaHeaders.OFFSET)
        );
    }


}
