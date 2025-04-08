package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import jdk.jshell.spi.ExecutionControl;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CheckoutProcessingWorkerTest {

    @Mock
    private TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    @Mock
    private CheckoutRegistrationWorker checkoutRegistrationWorker;

    @InjectMocks
    private CheckoutProcessingWorker checkoutProcessingWorker;

    static Stream<Arguments> generateData() {
        return Stream.of(
            Arguments.of(Collections.emptyList()),
            Arguments.of(Instancio
                .ofList(TransactionalInboxOrder.class).size(10).create())
        );
    }

    @Test
    void testProcessCheckout() {
        Assertions.assertThrows(ExecutionControl.NotImplementedException.class,
            () -> checkoutProcessingWorker.processCheckout("dummy"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generateData")
    void testRetrieveExistingOrder(List<TransactionalInboxOrder> orderList) {
        String mockWorkerId = "worker-1";
        Mockito.when(checkoutRegistrationWorker.getWorkerId())
            .thenReturn(mockWorkerId);
        Mockito.when(transactionalInboxOrderRepository
                .findByWorkerIdAndStatus(mockWorkerId, InboxOrderStatus.IN_PROGRESS))
            .thenReturn(orderList);

        var res = checkoutProcessingWorker.retrieveExistingOrder();

        Assertions.assertIterableEquals(orderList, res);
    }

    @Test
    void testPullNewOrder() {
        Assertions.assertThrows(ExecutionControl.NotImplementedException.class,
            checkoutProcessingWorker::pullNewOrder);
    }
}
