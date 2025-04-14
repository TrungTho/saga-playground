package com.saga.playground.checkoutservice.workers.checkout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.TestConstants;
import com.saga.playground.checkoutservice.configs.ObjectMapperConfig;
import com.saga.playground.checkoutservice.constants.ConsumerConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import org.apache.logging.log4j.util.Strings;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.logging.LogManager;
import java.util.stream.Stream;

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Import({
    ObjectMapperConfig.class,
    CheckoutHelper.class
})
class CheckoutHelperTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CheckoutHelper checkoutHelper;

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

    @Test
    void testBuildCheckoutInfo_OK(CapturedOutput output) {
        String orderId = "1";
        String rawPayload = TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId);

        TransactionalInboxOrder mockInbox = new TransactionalInboxOrder(orderId, rawPayload);
        var res = Assertions.assertDoesNotThrow(
            () -> checkoutHelper.buildCheckoutInfo(mockInbox)
        );

        Assertions.assertEquals(orderId, res.getOrderId(), "ID should match");
        Assertions.assertEquals("jlZHXEryFFDNnRPWXFKjtSNcg", res.getUserId(), "UserID should match");
        Assertions.assertEquals(PaymentStatus.INIT, res.getCheckoutStatus(), "Status should match");
        Assertions.assertNotNull(res.getAmount(), "Amount should match");
        Assertions.assertEquals(-1, BigDecimal.ZERO.compareTo(res.getAmount()),
            "Amount should be greater than 0");
    }

    @ParameterizedTest(name = "{1} ### {2}")
    @MethodSource("generateData")
    void testDecodeAmount(int orderId, String encodedVal, BigDecimal expectedVal) {
        var res = checkoutHelper.decodeAmount(encodedVal);
        Assertions.assertEquals(expectedVal.setScale(ConsumerConstant.ORDER_AMOUNT_SCALE), res,
            "Test %d should success".formatted(orderId));
    }

    @Test
    void testBuildCheckoutInfo_Failed(CapturedOutput output) {
        String orderId = "1";
        String rawPayload = TestConstants.MOCK_CDC_PAYLOAD; // error with %s placeholder
        TransactionalInboxOrder mockInbox = new TransactionalInboxOrder(orderId, rawPayload);

        Assertions.assertThrows(JsonProcessingException.class,
            () -> checkoutHelper.buildCheckoutInfo(mockInbox)
        );
    }

    @Test
    void testCheckout_OK(CapturedOutput output) {
        var mockCheckout = Instancio.of(Checkout.class).create();

        var res = Assertions.assertDoesNotThrow(
            () -> checkoutHelper.checkout(mockCheckout)
        );

        Assertions.assertTrue(
            output.toString().contains("Call payment gateway for checking out order %s"
                .formatted(mockCheckout.getOrderId())));
        Assertions.assertFalse(Strings.isBlank(res), "Checkout should return session id");
    }

    @Test
    void testCheckout_Failed(CapturedOutput output) {
        var mockCheckout = Mockito.mock(Checkout.class);
        Mockito.when(mockCheckout.getOrderId())
            .thenThrow(new RuntimeException())
            .thenReturn("1");

        Assertions.assertThrows(RuntimeException.class,
            () -> checkoutHelper.checkout(mockCheckout)
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
            () -> checkoutHelper.postCheckoutProcess(mockCheckout)
        );

        Assertions.assertTrue(output.toString().contains("POST_CHECKOUT %s"
            .formatted(mockCheckout.getOrderId())));
    }

}
