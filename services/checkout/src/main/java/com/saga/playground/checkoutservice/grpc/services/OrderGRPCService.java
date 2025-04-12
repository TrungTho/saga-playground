package com.saga.playground.checkoutservice.grpc.services;

import com.saga.playground.checkoutservice.grpc.protobufs.OrderServiceGrpc;
import com.saga.playground.checkoutservice.grpc.protobufs.SwitchToPendingPaymentRequest;
import com.saga.playground.checkoutservice.grpc.protobufs.SwitchToPendingPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderGRPCService {

    @GrpcClient("order-service")
    private OrderServiceGrpc.OrderServiceBlockingStub stub;

    // instead of catch and handle exception here, we will delegate it to the upstream method
    public void switchOrderStatus(int orderId) {
        log.info("Make gRPC call to switch status of order {}", orderId);
        SwitchToPendingPaymentRequest request = SwitchToPendingPaymentRequest
            .newBuilder()
            .setId(orderId)
            .build();

        SwitchToPendingPaymentResponse res =
            stub.switchOrderToPendingPayment(request);
        log.info("Successfully switch order {} to {}",
            res.getOrder().getId(), res.getOrder().getStatus());
    }

}
