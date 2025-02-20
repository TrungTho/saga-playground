package com.saga.playground.checkoutservice.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.configs.ObjectMapperConfig;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Import({
        ObjectMapperConfig.class,
        CheckoutInboxWorker.class,
})
class CheckoutInboxWorkerTest {
    private final TransactionalInboxOrder mockDbLogs =
            new TransactionalInboxOrder("1", "{\"key\":\"dummyValue\"}");

    private final String mockPayload = "{\"payload\":{\"after\":{\"id\":%s," +
            "\"user_id\":\"jlZHXEryFFDNnRPWXFKjtSNcg\"," +
            "\"status\":\"created\",\"amount\":\"B68=\",\"message\":\"\"," +
            "\"created_at\":\"2025-01-26T14:38:18.741171Z\"," +
            "\"updated_at\":\"2025-01-26T14:38:18.741171Z\"}}}";
    @MockitoBean
    private TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CheckoutInboxWorker checkoutInboxWorker;

    private Message<byte[]> generateNewMessage(String id) {
        return new Message<byte[]>() {
            @Override
            public byte[] getPayload() {
                return mockPayload.formatted(id).getBytes(StandardCharsets.US_ASCII);
            }

            @Override
            public MessageHeaders getHeaders() {
                return null;
            }
        };
    }

    @Test
    void bulkSaveMessages_OK(CapturedOutput output) {
        final int numberOfMessages = 10;
        List<Message<byte[]>> messageList = new ArrayList<>();
        for (int i = 0; i < numberOfMessages; i++) {
            messageList.add(generateNewMessage("%d".formatted(i)));
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
    void bulkSaveMessages_Error(CapturedOutput output) {
        final int numberOfMessages = 10;
        List<Message<byte[]>> messageList = new ArrayList<>();
        for (int i = 0; i < numberOfMessages; i++) {
            messageList.add(generateNewMessage("%d".formatted(i)));
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
    void extractPayloadFromMessage() {
        String mockId = "67";
        String s = mockPayload.formatted(mockId);

        Message<byte[]> mockMsg = new Message<byte[]>() {
            @Override
            public byte[] getPayload() {
                return s.getBytes(StandardCharsets.US_ASCII);
            }

            @Override
            public MessageHeaders getHeaders() {
                return null;
            }
        };

        List<String> results = checkoutInboxWorker.extractPayloadFromMessage(mockMsg);
        Assertions.assertNotSame(0, results.size(),
                "Result should not be empty list");
        Assertions.assertEquals(mockId, results.get(0), "ID should match");
    }
}