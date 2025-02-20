package com.saga.playground.checkoutservice.infrastructure.repositories;

import com.saga.playground.checkoutservice.ContainerBaseTest;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionalInboxOrderRepositoryTest extends ContainerBaseTest {
    private final TransactionalInboxOrder mockDbLogs =
            new TransactionalInboxOrder("1", "{\"key\":\"dummyValue\"}");

    @Autowired
    private TransactionalInboxOrderRepository transactionalInboxOrderRepository;


    @BeforeEach
    void deleteMockRecord() {
        transactionalInboxOrderRepository.deleteAll();
    }

    private TransactionalInboxOrder saveMockRecord() {
        return transactionalInboxOrderRepository.save(mockDbLogs);
    }

    @Test
    void testInsertData_OK() {
        var savedRecord = saveMockRecord();
        Assertions.assertEquals(mockDbLogs.getId(), savedRecord.getId(),
                "ID should match");

        Assertions.assertEquals(mockDbLogs.getPayload(), savedRecord.getPayload(),
                "Payload should match");
    }

    @Test
    void testInsertData_Duplicate() {
        var savedRecord = saveMockRecord();

        Assertions.assertEquals(mockDbLogs.getId(), savedRecord.getId(),
                "ID should match");

        Assertions.assertEquals(mockDbLogs.getPayload(), savedRecord.getPayload(),
                "Payload should match");

        TransactionalInboxOrder duplicatedRecord =
                new TransactionalInboxOrder(mockDbLogs.getOrderId(), mockDbLogs.getPayload());
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            transactionalInboxOrderRepository.save(duplicatedRecord);
        }, "Exception should be thrown in case of order duplication");
    }

    @Test
    void testBulkInsertData_OK() {
        final int numberOfOrders = 10;
        List<TransactionalInboxOrder> orders = new ArrayList<>();

        for (int i = 0; i < numberOfOrders; i++) {
            var order = new TransactionalInboxOrder("%d".formatted(i), mockDbLogs.getPayload());
            orders.add(order);
        }

        long existingRecords = transactionalInboxOrderRepository.count();
        Assertions.assertEquals(0L, existingRecords, "Table should be empty before test is started");

        Assertions.assertDoesNotThrow(() -> {
            transactionalInboxOrderRepository.saveAllAndFlush(orders);
        }, "Exception should NOT be thrown in case of normal bulk save");

        // retrieve order to confirm they are saved
        for (int i = 0; i < numberOfOrders; i++) {
            var retrievedOrder = transactionalInboxOrderRepository.findByOrderId("%d".formatted(i));
            Assertions.assertTrue(retrievedOrder.isPresent(), "Order should be stored");
            Assertions.assertFalse(retrievedOrder.get().getOrderId().isEmpty(), "ID should not be empty");
            Assertions.assertFalse(retrievedOrder.get().getPayload().isEmpty(), "Payload should not be empty");
        }
    }

    @Test
    void testBulkInsertData_Duplicate() {
        TransactionalInboxOrder duplicatedRecord =
                new TransactionalInboxOrder(mockDbLogs.getOrderId(), mockDbLogs.getPayload());
        var orders = List.of(mockDbLogs, duplicatedRecord);

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            transactionalInboxOrderRepository.saveAllAndFlush(orders);
        }, "Exception should be thrown in case of order duplication");
    }

    @Test
    void testFindAndDeleteByOrderId() {
        var result = saveMockRecord();
        Assertions.assertNotNull(result,
                "Mock record should be saved successfully");

        var retrievedRecord = transactionalInboxOrderRepository.findByOrderId(mockDbLogs.getOrderId());
        Assertions.assertTrue(retrievedRecord.isPresent(),
                "Mock record should be retrieved successfully");

        transactionalInboxOrderRepository.deleteByOrderId(mockDbLogs.getOrderId());
        retrievedRecord = transactionalInboxOrderRepository.findByOrderId(mockDbLogs.getOrderId());
        Assertions.assertTrue(retrievedRecord.isEmpty(),
                "Mock record should not be found after deletion");
    }

    @Test
    void testDeleteTestData() {
        var mockRecord2 = mockDbLogs;
        mockRecord2.setOrderId("2");
        var results = transactionalInboxOrderRepository.saveAll(List.of(mockDbLogs, mockRecord2));

        Assertions.assertEquals(2, results.size());
        for (var item : results) {
            Assertions.assertNotNull(item, "Mock record should not be null");
        }

        var retrievedRecord = transactionalInboxOrderRepository.findByOrderId(mockRecord2.getOrderId());
        Assertions.assertTrue(retrievedRecord.isPresent(),
                "Mock record should be retrieved successfully");


        transactionalInboxOrderRepository.deleteAll();

        retrievedRecord = transactionalInboxOrderRepository.findByOrderId(mockRecord2.getOrderId());
        Assertions.assertTrue(retrievedRecord.isEmpty(),
                "Mock record should NOT be retrieved after deletion");

    }
}