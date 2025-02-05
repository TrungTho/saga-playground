package com.saga.playground.checkoutservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableKafka
public class KafkaConfig {

    // configurations
    @Bean
    public RecordMessageConverter converter() {
        return new JsonMessageConverter();
    }

    // consumers registration
    @KafkaListener(groupId = "checkout_service:created_order", topics = "hihi")
    public void pullCreatedOrder(String in) {
        log.info("Received message: {}", in);
    }


}
