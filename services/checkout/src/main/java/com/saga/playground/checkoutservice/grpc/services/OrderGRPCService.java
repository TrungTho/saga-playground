package com.example.playground.services;

import com.example.playground.protobuf.OrderServiceGrpc;
import com.example.playground.protobuf.SwitchToPendingPaymentRequest;
import com.example.playground.protobuf.SwitchToPendingPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderGRPCService {

    @GrpcClient("order-service")
    private OrderServiceGrpc.OrderServiceBlockingStub stub;

    public void greet(int orderId) {
        SwitchToPendingPaymentRequest request = SwitchToPendingPaymentRequest
            .newBuilder()
            .setId(orderId)
            .build();

        SwitchToPendingPaymentResponse res =
            stub.switchOrderToPendingPayment(request);

        log.info("This is the response from grpc server {}", res);
    }

}
