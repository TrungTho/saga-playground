package com.saga.playground.checkoutservice.kafka;

import com.saga.playground.checkoutservice.constants.ConsumerConstant;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.workers.inboxpatterns.CheckoutInboxWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ExtendWith({OutputCaptureExtension.class, SpringExtension.class})
@ActiveProfiles("test")
@EmbeddedKafka(topics = ConsumerConstant.ORDER_CREATED_TOPIC)
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // we don't want to init multiple brokers
@Import({KafkaListenerRegistrations.class, KafkaConfig.class})
class KafkaListenerRegistrationsTest {

    private final TransactionalInboxOrder mockPayload =
        new TransactionalInboxOrder("1", "{\"key\":\"dummyValue\"}");

    @MockitoBean
    private CheckoutInboxWorker checkoutInboxWorker;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KafkaListenerRegistrations kafkaListenerRegistrations;

    @BeforeEach
    public void setUp() {
        embeddedKafka.brokerProperties(Map.of("controlled.shutdown.enable", "true"));

        for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry
            .getListenerContainers()) {
            System.err.println(messageListenerContainer.getContainerProperties());
            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafka.getPartitionsPerTopic());
        }
    }

    @Test
    void testPullCreatedOrder_OK(CapturedOutput output) {
        ArgumentCaptor<List<Message<String>>> captor = ArgumentCaptor.forClass(List.class);

        kafkaTemplate.send(ConsumerConstant.ORDER_CREATED_TOPIC,
            mockPayload.toString());
        kafkaTemplate.send(ConsumerConstant.ORDER_CREATED_TOPIC,
            mockPayload.toString());
        kafkaTemplate.send(ConsumerConstant.ORDER_CREATED_TOPIC,
            mockPayload.toString());

        System.out.println("Successfully send message to topic");

        Mockito.verify(checkoutInboxWorker, Mockito.timeout(10_000).times(1))
            .bulkSaveMessages(captor.capture());

        var listParams = captor.getValue();

        Awaitility.await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);

        Assertions.assertEquals(3, listParams.size(), "Number of params should be 3");
        Assertions.assertTrue(output.toString().contains("INBOX_ORDER_START"));
        Assertions.assertTrue(output.toString().contains("INBOX_ORDER_ACK"));
        Assertions.assertTrue(output.toString().contains("INBOX_ORDER_FINISHED"));
    }

}
