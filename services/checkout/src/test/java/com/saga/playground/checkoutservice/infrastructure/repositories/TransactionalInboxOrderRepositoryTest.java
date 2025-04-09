package com.saga.playground.checkoutservice.infrastructure.repositories;

import com.saga.playground.checkoutservice.basetest.PostgresContainerBaseTest;
import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;


// instead of using embedded H2 db for faster test bootstrapping
// we will use a real postgres container instead, because we have preLiquibase and Liquibase migration
// therefore we also want to ensure that those migration configurations is valid along with the code
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionalInboxOrderRepositoryTest extends PostgresContainerBaseTest {
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

        String workerId = "123";
        savedRecord.setWorkerId(workerId);
        transactionalInboxOrderRepository.save(savedRecord);

        var retrievedInbox = transactionalInboxOrderRepository.findByOrderId(mockDbLogs.getOrderId());
        Assertions.assertTrue(retrievedInbox.isPresent(), "Record should be load successfully");
        Assertions.assertEquals(workerId, retrievedInbox.get().getWorkerId(), "worker id should be matched");
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

    @Test
    void testFindByWorkerId() {
        // check table contains to record
        var res = transactionalInboxOrderRepository.findAll();
        Assertions.assertTrue(res.isEmpty(),
            "Table should contain no record when test starts");

        // insert some dummy data
        int numberOfRecords = 10;
        String workerId = "worker-1";
        List<TransactionalInboxOrder> mockInboxes = new ArrayList<>();
        for (int i = 1; i <= numberOfRecords; i++) {
            var item = new TransactionalInboxOrder("%d".formatted(i), mockDbLogs.getPayload());

            if (i % 2 == 0) {
                item.setWorkerId(workerId);
            }

            mockInboxes.add(item);
        }

        mockInboxes.get(1).setStatus(InboxOrderStatus.IN_PROGRESS);

        transactionalInboxOrderRepository.saveAllAndFlush(mockInboxes);

        res = transactionalInboxOrderRepository.findAll();
        Assertions.assertEquals(numberOfRecords, res.size(),
            "Inserted orders should be equal expected number");

        var workerRecords = transactionalInboxOrderRepository
            .findByWorkerIdAndStatus(workerId, InboxOrderStatus.IN_PROGRESS);
        Assertions.assertEquals(1, workerRecords.size(),
            "number of record has worker id should match");
        var newRecords = transactionalInboxOrderRepository
            .findByWorkerIdAndStatus(workerId, InboxOrderStatus.NEW);
        Assertions.assertEquals(numberOfRecords / 2 - 1, newRecords.size(),
            "number of new record should match");
        var orphanedRecords = transactionalInboxOrderRepository
            .findByWorkerIdAndStatus(null, InboxOrderStatus.NEW);
        Assertions.assertEquals(numberOfRecords / 2, orphanedRecords.size(),
            "number of orphaned record should match");
    }

    @Test
    void testPullNewWorker() {
        // check table contains to record
        var res = transactionalInboxOrderRepository.findAll();
        Assertions.assertTrue(res.isEmpty(),
            "Table should contain no record when test starts");

        // insert some dummy data
        int numberOfRecords = 10;
        var mockOrders = Instancio.ofList(TransactionalInboxOrder.class)
            .size(numberOfRecords)
            .ignore(Select.field(TransactionalInboxOrder::getId))
            .ignore(Select.field(TransactionalInboxOrder::getCreatedAt))
            .ignore(Select.field(TransactionalInboxOrder::getUpdatedAt))
            .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.IN_PROGRESS)
            .set(Select.field(TransactionalInboxOrder::getPayload), mockDbLogs.getPayload())
            .create();


        transactionalInboxOrderRepository.saveAllAndFlush(mockOrders);

        res = transactionalInboxOrderRepository.findNewOrders();
        Assertions.assertTrue(res.isEmpty(), "No order should be found");

        mockOrders.get(0).setWorkerId("");
        for (int i = 0; i < numberOfRecords; i++) {
            if (i % 2 == 0) {
                if (i != 0) {
                    mockOrders.get(i).setWorkerId(null);
                }
                mockOrders.get(i).setStatus(InboxOrderStatus.NEW);
            }
        }

        // confirm the mocked data
        for (int i = 0; i < numberOfRecords; i++) {
            if (i % 2 == 0) {
                if (i != 0) {
                    Assertions.assertNull(mockOrders.get(i).getWorkerId());
                } else {
                    Assertions.assertEquals("", mockOrders.get(i).getWorkerId());
                }
            }
        }

        transactionalInboxOrderRepository.saveAllAndFlush(mockOrders);

        res = transactionalInboxOrderRepository.findNewOrders();
        Assertions.assertEquals(numberOfRecords / 2, res.size(),
            "Processable orders should be successfully retrieved");

        res = transactionalInboxOrderRepository.findAll();
        Assertions.assertEquals(numberOfRecords, res.size(),
            "Overall records should be stable");
    }
}
