package com.saga.playground.checkoutservice.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.constants.ErrorConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.presentations.responses.IPNResponse;
import com.saga.playground.checkoutservice.utils.http.error.HttpException;
import lombok.SneakyThrows;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PaymentGatewayHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CheckoutRepository checkoutRepository;

    @InjectMocks
    private PaymentGatewayHandler paymentGatewayHandler;

    @Test
    void testDecryptIPNResponse() {
        var mockRes = Instancio.of(IPNResponse.class).create();

        var res = paymentGatewayHandler.decryptIPNResponse(mockRes);

        Assertions.assertEquals(mockRes, res);
    }

    @Test
    void testValidateIPNResponse() {
        var mockRes = Instancio.of(IPNResponse.class).create();

        var res = paymentGatewayHandler.validateIPNResponse(mockRes);

        Assertions.assertTrue(res);
    }

    @Test
    void testPersistIPNResponse_CheckoutNotFound() {
        var mockRes = Instancio.of(IPNResponse.class).create();
        Mockito.when(checkoutRepository.findByOrderId(mockRes.orderId()))
            .thenReturn(Optional.empty());

        var err = Assertions.assertThrows(HttpException.class,
            () -> paymentGatewayHandler.persistIPNResponse(mockRes));
        Assertions.assertEquals(ErrorConstant.CODE_NOT_FOUND_ERROR,
            err.getError().getCode());
    }

    @SneakyThrows
    @Test
    void testPersistIPNResponse_OK() {
        ArgumentCaptor<Checkout> captor = ArgumentCaptor.forClass(Checkout.class);

        var mockRes = Instancio.of(IPNResponse.class)
            .set(Select.field(IPNResponse::status), PaymentStatus.FINALIZED)
            .create();
        var mockCheckout = Instancio.of(Checkout.class)
            .ignore(Select.field(Checkout::getWebhookPayload))
            .set(Select.field(Checkout::getOrderId), mockRes.orderId())
            .set(Select.field(Checkout::getCheckoutStatus), PaymentStatus.PROCESSING)
            .create();

        Mockito.when(checkoutRepository.findByOrderId(mockRes.orderId()))
            .thenReturn(Optional.of(mockCheckout));
        Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
            .thenReturn("Dummy");

        Assertions.assertDoesNotThrow(
            () -> paymentGatewayHandler.persistIPNResponse(mockRes));

        Mockito.verify(checkoutRepository, Mockito.times(1))
            .save(captor.capture());
        Assertions.assertEquals(mockRes.status(), captor.getValue().getCheckoutStatus());
        Assertions.assertNotNull(captor.getValue().getWebhookPayload());
    }

    @SneakyThrows
    @Test
    void testSimulateWebhookReceive(CapturedOutput output) {
        var mockRes = Instancio.of(IPNResponse.class)
            .set(Select.field(IPNResponse::status), PaymentStatus.FINALIZED)
            .create();
        var mockCheckout = Instancio.of(Checkout.class)
            .ignore(Select.field(Checkout::getWebhookPayload))
            .set(Select.field(Checkout::getOrderId), mockRes.orderId())
            .set(Select.field(Checkout::getCheckoutStatus), PaymentStatus.PROCESSING)
            .create();

        Mockito.when(checkoutRepository.findByOrderId(mockRes.orderId()))
            .thenReturn(Optional.of(mockCheckout));
        Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
            .thenReturn("Dummy");

        Assertions.assertDoesNotThrow(
            () -> paymentGatewayHandler.simulateWebhookReceived(mockRes));
        Assertions.assertTrue(output.toString().contains("Received IPN"));
    }

}
