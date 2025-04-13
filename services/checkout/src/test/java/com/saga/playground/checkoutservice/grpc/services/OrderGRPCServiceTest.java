package com.saga.playground.checkoutservice.grpc.services;

import com.saga.playground.checkoutservice.grpc.protobufs.OrderServiceGrpc;
import com.saga.playground.checkoutservice.grpc.protobufs.SwitchToPendingPaymentResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class OrderGRPCServiceTest {

    @Mock
    private OrderServiceGrpc.OrderServiceBlockingStub stub;

    @InjectMocks
    private OrderGRPCService orderGRPCService;

    @Test
    void switchOrderStatus_OK(CapturedOutput output) {
        Mockito.when(stub.switchOrderToPendingPayment(Mockito.any()))
            .thenReturn(SwitchToPendingPaymentResponse.newBuilder().build());

        int orderId = 1;
        Assertions.assertDoesNotThrow(() -> orderGRPCService.switchOrderStatus(orderId));

        Assertions.assertTrue(output.toString().contains("Successfully switch order"));
    }

}
