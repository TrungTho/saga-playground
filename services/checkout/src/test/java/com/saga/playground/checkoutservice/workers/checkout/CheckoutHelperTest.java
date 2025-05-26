package com.saga.playground.checkoutservice.workers.checkout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.TestConstants;
import com.saga.playground.checkoutservice.configs.ObjectMapperConfig;
import com.saga.playground.checkoutservice.configs.ThreadPoolConfig;
import com.saga.playground.checkoutservice.constants.MessageBrokerConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.presentations.requests.KafkaCreatedOrderMessage;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.logging.LogManager;
import java.util.stream.Stream;

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Import({
    ObjectMapperConfig.class,
    CheckoutHelper.class,
    ThreadPoolConfig.class,
})
class CheckoutHelperTest {

    @MockitoBean
    private CheckoutRepository checkoutRepository;

    @MockitoSpyBean
    private ObjectMapper objectMapper;

    @Autowired
    private CheckoutHelper checkoutHelper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    static Stream<Arguments> generateData() {
        return Stream.of(
            Arguments.of(120, "Hxk=", BigDecimal.valueOf(79.61)),
            Arguments.of(122, "FXY=", BigDecimal.valueOf(54.94)),
            Arguments.of(123, "Fbc=", BigDecimal.valueOf(55.59)),
            Arguments.of(115, "H/4=", BigDecimal.valueOf(81.90)),
            Arguments.of(116, "EXo=", BigDecimal.valueOf(44.74)),
            Arguments.of(117, "B1Y=", BigDecimal.valueOf(18.78)),
            Arguments.of(118, "I38=", BigDecimal.valueOf(90.87)),
            Arguments.of(119, "A/Y=", BigDecimal.valueOf(10.14)),
            Arguments.of(121, "Eb4=", BigDecimal.valueOf(45.42))
        );
    }

    @AfterEach
    void reset() throws Exception {
        LogManager.getLogManager().readConfiguration();
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(
        value = PaymentStatus.class,
        names = {"INIT", "PROCESSING"},
        mode = EnumSource.Mode.INCLUDE
    )
    void testUpsertCheckoutInfo_ExistingValidCheckout(PaymentStatus status) {
        String orderId = "1";
        String rawPayload = TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId);
        var mockCheckout = Instancio.of(Checkout.class).create();
        mockCheckout.setCheckoutStatus(status);

        Mockito.when(checkoutRepository.findByOrderId(orderId))
            .thenReturn(Optional.of(mockCheckout));

        TransactionalInboxOrder mockInbox = new TransactionalInboxOrder(orderId, rawPayload);
        var res = Assertions.assertDoesNotThrow(
            () -> checkoutHelper.upsertCheckoutInfo(mockInbox)
        );

        // verify no new record is created
        Assertions.assertFalse(res.isEmpty());
        Assertions.assertSame(mockCheckout, res.get());
        Mockito.verify(objectMapper, Mockito.times(0))
            .readValue(rawPayload, KafkaCreatedOrderMessage.class);
    }


    @SneakyThrows
    @Test
    void testUpsertCheckoutInfo_ExistingInvalidCheckout(CapturedOutput output) {
        String orderId = "1";
        String rawPayload = TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId);
        var mockCheckout = Instancio.of(Checkout.class).create();
        mockCheckout.setCheckoutStatus(PaymentStatus.FAILED);

        Mockito.when(checkoutRepository.findByOrderId(orderId))
            .thenReturn(Optional.of(mockCheckout));

        TransactionalInboxOrder mockInbox = new TransactionalInboxOrder(orderId, rawPayload);
        var res = Assertions.assertDoesNotThrow(
            () -> checkoutHelper.upsertCheckoutInfo(mockInbox)
        );

        // verify no new record is created
        Assertions.assertTrue(res.isEmpty());
        Assertions.assertTrue(output.toString().contains("INVALID CHECKOUT STATE"));
        Mockito.verify(objectMapper, Mockito.times(0))
            .readValue(rawPayload, KafkaCreatedOrderMessage.class);
    }

    @SneakyThrows
    @Test
    void testUpsertCheckoutInfo_OK(CapturedOutput output) {
        String orderId = "1";
        String rawPayload = TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId);

        TransactionalInboxOrder mockInbox = new TransactionalInboxOrder(orderId, rawPayload);
        var res = Assertions.assertDoesNotThrow(
            () -> checkoutHelper.upsertCheckoutInfo(mockInbox)
        );

        Assertions.assertTrue(res.isPresent());
        Assertions.assertEquals(orderId, res.get().getOrderId(), "ID should match");
        Assertions.assertEquals("jlZHXEryFFDNnRPWXFKjtSNcg", res.get().getUserId(), "UserID should match");
        Assertions.assertEquals(PaymentStatus.INIT, res.get().getCheckoutStatus(), "Status should match");
        Assertions.assertNotNull(res.get().getAmount(), "Amount should match");
        Assertions.assertEquals(-1, BigDecimal.ZERO.compareTo(res.get().getAmount()),
            "Amount should be greater than 0");

        Mockito.verify(checkoutRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(objectMapper, Mockito.times(1))
            .readValue(rawPayload, KafkaCreatedOrderMessage.class);
    }

    @ParameterizedTest(name = "{1} ### {2}")
    @MethodSource("generateData")
    void testDecodeAmount(int orderId, String encodedVal, BigDecimal expectedVal) {
        var res = checkoutHelper.decodeAmount(encodedVal);
        Assertions.assertEquals(expectedVal.setScale(MessageBrokerConstant.ORDER_AMOUNT_SCALE), res,
            "Test %d should success".formatted(orderId));
    }

    @Test
    void testUpsertCheckoutInfo_Failed(CapturedOutput output) {
        String orderId = "1";
        String rawPayload = TestConstants.MOCK_CDC_PAYLOAD; // error with %s placeholder
        TransactionalInboxOrder mockInbox = new TransactionalInboxOrder(orderId, rawPayload);

        Assertions.assertThrows(JsonProcessingException.class,
            () -> checkoutHelper.upsertCheckoutInfo(mockInbox)
        );
    }

    @Test
    void testRegisterCheckout_OK(CapturedOutput output) {
        var mockCheckout = Instancio.of(Checkout.class).create();

        var res = Assertions.assertDoesNotThrow(
            () -> checkoutHelper.registerCheckout(mockCheckout)
        );

        Assertions.assertTrue(
            output.toString().contains("Call payment gateway for checking out order %s"
                .formatted(mockCheckout.getOrderId())));
        Assertions.assertFalse(Strings.isBlank(res), "Checkout should return session id");
    }

    @Test
    void testRegisterCheckout_Failed(CapturedOutput output) {
        var mockCheckout = Mockito.mock(Checkout.class);
        Mockito.when(mockCheckout.getOrderId())
            .thenThrow(new RuntimeException())
            .thenReturn("1");

        Assertions.assertThrows(RuntimeException.class,
            () -> checkoutHelper.registerCheckout(mockCheckout)
        );

        Assertions.assertFalse(
            output.toString().contains("Call payment gateway for checking out order"));
        Assertions.assertTrue(
            output.toString().contains("Error when call payment gateway for order"));
    }

    @Test
    void testPostCheckoutProcess(CapturedOutput output) {
        var mockCheckout = Instancio.of(Checkout.class).create();

        Assertions.assertDoesNotThrow(
            () -> checkoutHelper.postCheckoutProcess(mockCheckout.getOrderId())
        );

        Assertions.assertTrue(output.toString().contains("POST_CHECKOUT %s"
            .formatted(mockCheckout.getOrderId())));
    }

}
