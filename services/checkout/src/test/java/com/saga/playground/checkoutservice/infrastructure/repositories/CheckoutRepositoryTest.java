package com.saga.playground.checkoutservice.infrastructure.repositories;

import com.saga.playground.checkoutservice.basetest.PostgresContainerBaseTest;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;


// instead of using embedded H2 db for faster test bootstrapping
// we will use a real postgres container instead, because we have preLiquibase and Liquibase migration
// therefore we also want to ensure that those migration configurations is valid along with the code
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CheckoutRepositoryTest extends PostgresContainerBaseTest {

    @Autowired
    private CheckoutRepository checkoutRepository;

    @Test
    void testBasicQueries() {
        var checkouts = checkoutRepository.findAll();
        Assertions.assertTrue(checkouts.isEmpty(),
            "Table should be empty before starting the test");
        Checkout randomValues = Instancio.of(Checkout.class).create();

        Checkout checkoutEntity = new Checkout(
            randomValues.getOrderId(),
            randomValues.getUserId(),
            randomValues.getCheckoutStatus(),
            randomValues.getAmount()
        );

        Assertions.assertDoesNotThrow(() ->
            checkoutRepository.save(checkoutEntity)
        );

        var retrievedRecords = checkoutRepository.findAll();
        Assertions.assertFalse(retrievedRecords.isEmpty(),
            "Table should contain record now");
        Assertions.assertEquals(1, retrievedRecords.size(),
            "Table should only contain 1 record");

        Assertions.assertNotNull(retrievedRecords.get(0).getId(),
            "ID should be auto generated");
        Assertions.assertEquals(randomValues.getOrderId(), retrievedRecords.get(0).getOrderId());
        Assertions.assertEquals(randomValues.getUserId(), retrievedRecords.get(0).getUserId());
        Assertions.assertEquals(randomValues.getCheckoutStatus().toString(),
            retrievedRecords.get(0).getCheckoutStatus().toString());
        Assertions.assertEquals(randomValues.getAmount(), retrievedRecords.get(0).getAmount());
    }

    @Test
    void testFindByOrderId() {
        // verify empty table first
        var res = checkoutRepository.findAll();
        Assertions.assertTrue(res.isEmpty(),
            "Table should be empty before starting the test");
        int numberOfRecord = 10;
        var mockCheckouts = Instancio.ofList(Checkout.class)
            .size(numberOfRecord)
            .ignore(Select.field(Checkout::getId))
            .ignore(Select.field(Checkout::getWebhookPayload))
            .create();

        checkoutRepository.saveAll(mockCheckouts);

        // find first record
        var expectedItem = mockCheckouts.get(0);
        var item = checkoutRepository.findByOrderId(expectedItem.getOrderId());

        Assertions.assertTrue(item.isPresent(),
            "Record should be presented");
        Assertions.assertEquals(expectedItem.getId(), item.get().getId());
        Assertions.assertEquals(expectedItem.getUpdatedAt(), item.get().getUpdatedAt());
    }

    @Test
    void testfindTop100ByCheckoutStatusAndEventPublished() {
        // verify empty table first
        var res = checkoutRepository.findAll();
        Assertions.assertTrue(res.isEmpty(),
            "Table should be empty before starting the test");
        int numberOfRecord = 10;
        var mockCheckouts = Instancio.ofList(Checkout.class)
            .size(numberOfRecord)
            .set(Select.field(Checkout::getCheckoutStatus), PaymentStatus.INIT)
            .set(Select.field(Checkout::getEventPublished), false)
            .ignore(Select.field(Checkout::getId))
            .ignore(Select.field(Checkout::getWebhookPayload))
            .create();

        mockCheckouts.get(0).setCheckoutStatus(PaymentStatus.PROCESSING);
        mockCheckouts.get(1).setCheckoutStatus(PaymentStatus.FINALIZED);
        mockCheckouts.get(2).setCheckoutStatus(PaymentStatus.FAILED);

        checkoutRepository.saveAll(mockCheckouts);

        res = checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(PaymentStatus.FAILED, false);
        Assertions.assertEquals(1, res.size());
        res = checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(PaymentStatus.FINALIZED, false);
        Assertions.assertEquals(1, res.size());

        mockCheckouts.forEach(item -> item.setEventPublished(true));

        checkoutRepository.saveAll(mockCheckouts);

        res = checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(PaymentStatus.FAILED, false);
        Assertions.assertTrue(res.isEmpty(),
            "no records should be retrieved but there is %d".formatted(res.size()));

        res = checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(PaymentStatus.INIT, true);
        Assertions.assertEquals(7, res.size());
    }

}
