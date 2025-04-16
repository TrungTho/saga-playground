package com.saga.playground.checkoutservice.application.impl;

import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.presentations.responses.IPNResponse;
import com.saga.playground.checkoutservice.utils.http.error.HttpException;
import com.saga.playground.checkoutservice.webhooks.PaymentGatewayHandler;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CheckoutRestServiceImplTest {

    @Mock
    private PaymentGatewayHandler paymentGatewayHandler;

    @Mock
    private CheckoutRepository checkoutRepository;

    @InjectMocks
    private CheckoutRestServiceImpl checkoutRestService;

    @Test
    void testGetCheckoutStatus_CheckoutNotFound() {
        Mockito.when(checkoutRepository.findByOrderId(Mockito.any()))
            .thenReturn(Optional.empty());

        Assertions.assertThrows(HttpException.class,
            () -> checkoutRestService.getCheckoutStatus("1"));
    }

    @Test
    void testGetCheckoutStatus_TerminalState() {
        String orderId = "1";
        var mockCheckout = Instancio.of(Checkout.class)
            .set(Select.field(Checkout::getOrderId), orderId)
            .set(Select.field(Checkout::getCheckoutStatus), PaymentStatus.FINALIZED)
            .create();

        Mockito.when(checkoutRepository.findByOrderId(Mockito.any()))
            .thenReturn(Optional.of(mockCheckout));

        var res = Assertions.assertDoesNotThrow(() -> checkoutRestService.getCheckoutStatus(orderId));
        Assertions.assertEquals(orderId, res.orderId());
        Assertions.assertEquals(PaymentStatus.FINALIZED, res.status());
        Assertions.assertTrue(res.isTerminated());
        Mockito.verify(paymentGatewayHandler, Mockito.times(0))
            .stimulateCallingPaymentGateway(Mockito.any());
    }

    @Test
    void testGetCheckoutStatus_NonTerminalStateUpdateCheckout() {
        String orderId = "1";
        var mockCheckout = Instancio.of(Checkout.class)
            .set(Select.field(Checkout::getOrderId), orderId)
            .set(Select.field(Checkout::getCheckoutStatus), PaymentStatus.PROCESSING)
            .create();

        var mockIPNResp = new IPNResponse(orderId, PaymentStatus.FINALIZED);

        Mockito.when(checkoutRepository.findByOrderId(Mockito.any()))
            .thenReturn(Optional.of(mockCheckout));
        Mockito.when(paymentGatewayHandler.stimulateCallingPaymentGateway(orderId))
            .thenReturn(mockIPNResp);

        var res = Assertions.assertDoesNotThrow(() -> checkoutRestService.getCheckoutStatus(orderId));

        Assertions.assertEquals(orderId, res.orderId());
        Assertions.assertEquals(mockIPNResp.status(), res.status());
        Assertions.assertTrue(res.isTerminated());
        Mockito.verify(checkoutRepository, Mockito.times(1)).save(Mockito.any());
    }


    @Test
    void testGetCheckoutStatus_NonTerminalStateNotUpdateCheckout() {
        String orderId = "1";
        var mockCheckout = Instancio.of(Checkout.class)
            .set(Select.field(Checkout::getOrderId), orderId)
            .set(Select.field(Checkout::getCheckoutStatus), PaymentStatus.PROCESSING)
            .create();

        var mockIPNResp = new IPNResponse(orderId, mockCheckout.getCheckoutStatus());

        Mockito.when(checkoutRepository.findByOrderId(Mockito.any()))
            .thenReturn(Optional.of(mockCheckout));
        Mockito.when(paymentGatewayHandler.stimulateCallingPaymentGateway(orderId))
            .thenReturn(mockIPNResp);

        var res = Assertions.assertDoesNotThrow(() -> checkoutRestService.getCheckoutStatus(orderId));

        Assertions.assertEquals(orderId, res.orderId());
        Assertions.assertEquals(mockIPNResp.status(), res.status());
        Assertions.assertFalse(res.isTerminated());
        Mockito.verify(checkoutRepository, Mockito.times(0)).save(Mockito.any());
    }

    @ParameterizedTest
    @EnumSource(
        value = PaymentStatus.class,
        names = {"FAILED", "FINALIZED"},
        mode = EnumSource.Mode.INCLUDE
    )
    void testIsTerminalState_True(PaymentStatus status) {
        var res = checkoutRestService.isTerminalState(status);
        Assertions.assertTrue(res);
    }

    @ParameterizedTest
    @EnumSource(
        value = PaymentStatus.class,
        names = {"FAILED", "FINALIZED"},
        mode = EnumSource.Mode.EXCLUDE
    )
    void testIsTerminalState_False(PaymentStatus status) {
        var res = checkoutRestService.isTerminalState(status);
        Assertions.assertFalse(res);
    }

}
