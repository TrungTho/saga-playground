package com.saga.playground.checkoutservice.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.configs.ObjectMapperConfig;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Import({
    ObjectMapperConfig.class,
    CheckoutInboxWorker.class,
})
class CheckoutInboxWorkerTest {
    private final TransactionalInboxOrder mockDbLogs =
        new TransactionalInboxOrder("1", "{\"key\":\"dummyValue\"}");

    private final String mockPayload = "{\"payload\":{\"after\":{\"id\":%s,"
        + "\"user_id\":\"jlZHXEryFFDNnRPWXFKjtSNcg\","
        + "\"status\":\"created\",\"amount\":\"B68=\",\"message\":\"\","
        + "\"created_at\":\"2025-01-26T14:38:18.741171Z\","
        + "\"updated_at\":\"2025-01-26T14:38:18.741171Z\"}}}";
    @MockitoBean
    private TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CheckoutInboxWorker checkoutInboxWorker;

    @Test
    void bulkSaveMessages_OK(CapturedOutput output) {
        final int numberOfMessages = 10;
        List<Message<String>> messageList = new ArrayList<>();
        for (int i = 0; i < numberOfMessages; i++) {
            messageList.add(createMsg(mockPayload.formatted(i)));
        }

        Mockito.when(transactionalInboxOrderRepository.saveAllAndFlush(Mockito.any()))
            .thenReturn(null);

        Assertions.assertDoesNotThrow(() -> {
            checkoutInboxWorker.bulkSaveMessages(messageList);
        });

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1))
            .saveAllAndFlush(Mockito.any());
        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(0))
            .save(Mockito.any());
        Assertions.assertTrue(output.toString().contains("INBOX_ORDER_BULK_SAVED"),
            "Successful message should be printed");
    }

    @Test
    void bulkSaveMessages_SkipMessage(CapturedOutput output) {
        List<Message<String>> messageList = new ArrayList<>();
        messageList.add(createMsg("dummyString"));

        Mockito.when(transactionalInboxOrderRepository.saveAllAndFlush(Mockito.any()))
            .thenReturn(null);

        Assertions.assertDoesNotThrow(() -> {
            checkoutInboxWorker.bulkSaveMessages(messageList);
        });

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(0))
            .saveAllAndFlush(Collections.emptyList());
        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(0))
            .save(Mockito.any());
        Assertions.assertTrue(output.toString().contains("INBOX_ORDER_EMPTY_MESSAGE"),
            "Empty message should be printed");
    }

    @Test
    void bulkSaveMessages_Error(CapturedOutput output) {
        final int numberOfMessages = 10;
        List<Message<String>> messageList = new ArrayList<>();
        for (int i = 0; i < numberOfMessages; i++) {
            messageList.add(createMsg(mockPayload.formatted(i)));
        }

        Mockito.when(transactionalInboxOrderRepository.saveAllAndFlush(Mockito.any()))
            .thenThrow(new RuntimeException());
        Mockito.when(transactionalInboxOrderRepository.save(Mockito.any()))
            .thenReturn(null);


        Assertions.assertDoesNotThrow(() -> {
            checkoutInboxWorker.bulkSaveMessages(messageList);
        });

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1))
            .saveAllAndFlush(Mockito.any());
        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(numberOfMessages))
            .save(Mockito.any());
        Assertions.assertFalse(output.toString().contains("INBOX_ORDER_BULK_SAVED"),
            "Successful message should NOT be printed");
        for (int i = 0; i < numberOfMessages; i++) {
            Assertions.assertTrue(output.toString().contains("INBOX_ORDER_SAVED %d".formatted(i)),
                "Single successful message should be printed");
        }
    }

    @Test
    void testSequentialSaveOrders_OK(CapturedOutput output) {
        Mockito.when(transactionalInboxOrderRepository.save(mockDbLogs)).thenReturn(mockDbLogs);

        Assertions.assertDoesNotThrow(() -> {
            checkoutInboxWorker.sequentialSaveOrders(List.of(mockDbLogs));
        });

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1)).save(mockDbLogs);
        Assertions.assertTrue(output.toString().contains("INBOX_ORDER_SAVED"),
            "Successful message should be printed");
    }

    @Test
    void testSequentialSaveOrders_Duplicate(CapturedOutput output) {
        Mockito.when(transactionalInboxOrderRepository.save(mockDbLogs))
            .thenThrow(new DataIntegrityViolationException("Violate unique constraint"));

        Assertions.assertDoesNotThrow(() -> {
            checkoutInboxWorker.sequentialSaveOrders(List.of(mockDbLogs));
        });

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1)).save(mockDbLogs);
        Assertions.assertTrue(output.toString().contains("INBOX_ORDER_SQL_ERROR"),
            "SQL error message should be printed");
        Assertions.assertFalse(output.toString().contains("INBOX_ORDER_SAVED"),
            "Successful message should NOT be printed");
    }

    @Test
    void testSequentialSaveOrders_UnhandledError(CapturedOutput output) {
        Mockito.when(transactionalInboxOrderRepository.save(mockDbLogs))
            .thenThrow(new RuntimeException());

        Assertions.assertDoesNotThrow(() -> {
            checkoutInboxWorker.sequentialSaveOrders(List.of(mockDbLogs));
        });

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1)).save(mockDbLogs);
        Assertions.assertTrue(output.toString().contains("INBOX_ORDER_SAVE_ERROR"),
            "Unhandled error message should be printed");
        Assertions.assertFalse(output.toString().contains("INBOX_ORDER_SAVED"),
            "Successful message should NOT be printed");
    }

    @Test
    void extractPayloadFromMessage_OK() {
        String mockId = "67";
        String s = mockPayload.formatted(mockId);

        Message<String> mockMsg = createMsg(s);

        List<String> results = checkoutInboxWorker.extractPayloadFromMessage(mockMsg);
        Assertions.assertNotSame(0, results.size(),
            "Result should not be empty list");
        Assertions.assertEquals(mockId, results.get(0), "ID should match");
    }

    @Test
    void extractPayloadFromMessage_Failed(CapturedOutput output) {
        String s = "Dummy string, obviously invalid json format";

        Message<String> mockMsg = createMsg(s);

        Assertions.assertDoesNotThrow(() -> checkoutInboxWorker.extractPayloadFromMessage(mockMsg));
        Assertions.assertTrue(output.toString().contains("Failed to parse payload from message"));
    }

    private Message<String> createMsg(String rawMsg) {
        return new Message<>() {
            @Override
            public @NotNull String getPayload() {
                return rawMsg;
            }

            @Override
            public @NotNull MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
    }

    @Test
    void extractPayloadFromMessage_MapperFromClass() throws JsonProcessingException {
        String mockId = "67";
        String s = mockPayload.formatted(mockId);

        ObjectMapper objectMapper = new ObjectMapperConfig().kafkaMessageObjectMapper();
        KafkaCreatedOrderMessage payload = objectMapper.readValue(s, KafkaCreatedOrderMessage.class);

        Assertions.assertEquals(Long.parseLong(mockId), payload.payload().after().id(),
            "ID should be equal");
    }

}
