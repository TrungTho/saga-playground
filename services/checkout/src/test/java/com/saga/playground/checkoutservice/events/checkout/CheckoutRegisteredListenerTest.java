package com.saga.playground.checkoutservice.events.checkout;

import com.saga.playground.checkoutservice.basetest.PostgresContainerBaseTest;
import com.saga.playground.checkoutservice.configs.EventListenerConfig;
import com.saga.playground.checkoutservice.configs.ObjectMapperConfig;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.webhooks.PaymentGatewayHandler;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.logging.LogManager;

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@EnableAsync
@Import({
    EventListenerConfig.class,
    PaymentGatewayHandler.class,
    CheckoutRegisteredListener.class,
    ObjectMapperConfig.class
})
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CheckoutRegisteredListenerTest extends PostgresContainerBaseTest {

    @Autowired
    private PaymentGatewayHandler paymentGatewayHandler;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private CheckoutRepository checkoutRepository;

    @AfterEach
    void reset() throws Exception {
        LogManager.getLogManager().readConfiguration();
    }

    @Test
    // in order for listener inside the other thread can see the mock record
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testCheckoutRegisteredHandler(CapturedOutput output) {
        // verify empty db
        var checkouts = checkoutRepository.findAll();
        Assertions.assertTrue(checkouts.isEmpty(), "Table should be empty");

        // prepare 1 record
        Checkout mockCheckout = Instancio.of(Checkout.class)
            .ignore(Select.field(Checkout::getId))
            .ignore(Select.field(Checkout::getWebhookPayload))
            .set(Select.field(Checkout::getCheckoutStatus), PaymentStatus.PROCESSING)
            .create();

        checkoutRepository.save(mockCheckout);

        final String orderId = mockCheckout.getOrderId();

        var event = new CheckoutRegisteredEvent(mockCheckout.getOrderId());
        applicationEventPublisher.publishEvent(event);

        Awaitility.await().until(
            () -> {
                final var item = checkoutRepository.findByOrderId(orderId)
                    .orElse(null);
                return item != null && item.getCheckoutStatus() == PaymentStatus.FINALIZED;
            });

        mockCheckout = Assertions.assertDoesNotThrow(() -> checkoutRepository.findByOrderId(orderId)
            .orElseThrow(RuntimeException::new));

        Assertions.assertTrue(output.toString()
            .contains("Registered order %s".formatted(orderId)));
        Assertions.assertTrue(output.toString()
            .contains("Received IPN for order %s".formatted(orderId)));
        Assertions.assertNotNull(mockCheckout.getWebhookPayload());
        Assertions.assertEquals(PaymentStatus.FINALIZED, mockCheckout.getCheckoutStatus());

        // clean up db
        checkoutRepository.delete(mockCheckout);
    }

}
