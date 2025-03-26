package com.saga.playground.checkoutservice.basetest;

import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class KafkaContainerBaseTest {

    protected static final KafkaContainer KAFKA_CONTAINER =
        new KafkaContainer("apache/kafka")
            .withExposedPorts(9092)
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094")
            .withStartupTimeout(Duration.ofMinutes(5))
            .withReuse(true);

    static {
        KAFKA_CONTAINER.start();
    }

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }
}
