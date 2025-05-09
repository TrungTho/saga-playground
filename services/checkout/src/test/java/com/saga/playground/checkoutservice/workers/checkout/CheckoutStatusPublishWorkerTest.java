package com.saga.playground.checkoutservice.workers.checkout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.playground.checkoutservice.constants.MessageBrokerConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.utils.locks.impl.ZookeeperDistributedLock;
import lombok.SneakyThrows;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CheckoutStatusPublishWorkerTest {

    @Mock
    private CheckoutRepository checkoutRepository;

    @Mock
    private ZookeeperDistributedLock distributedLock;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CheckoutStatusPublishWorker checkoutStatusPublishWorker;

    @Test
    void testPublishCheckoutStatus_AcquireLockFailed(CapturedOutput output) {
        Mockito.when(distributedLock.acquireLock(WorkerConstant.WORKER_CHECKOUT_STATUS_PUBLISH_LOCK))
            .thenReturn(false);

        checkoutStatusPublishWorker.publishCheckoutStatus();

        Assertions.assertTrue(output.toString().contains("Can't acquire lock to publish checkout status"));

        Mockito.verify(distributedLock, Mockito.times(0))
            .releaseLock(WorkerConstant.WORKER_CHECKOUT_STATUS_PUBLISH_LOCK);
    }

    @SneakyThrows
    @Test
    void testPublishCheckoutStatus_LockReleaseInCrash(CapturedOutput output) {
        Mockito.when(distributedLock.acquireLock(WorkerConstant.WORKER_CHECKOUT_STATUS_PUBLISH_LOCK))
            .thenReturn(true);
        Mockito.when(checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(Mockito.any(), Mockito.any()))
            .thenThrow(new RuntimeException());

        checkoutStatusPublishWorker.publishCheckoutStatus();

        Assertions.assertFalse(output.toString().contains("Can't acquire lock to publish checkout status"));

        Mockito.verify(distributedLock, Mockito.times(1))
            .releaseLock(WorkerConstant.WORKER_CHECKOUT_STATUS_PUBLISH_LOCK);
    }

    @Test
    void testGetCheckoutRecordForPublishing_EmptyList() {
        Mockito.when(checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(Mockito.any(), Mockito.any()))
            .thenReturn(Collections.emptyList());

        var res = checkoutStatusPublishWorker.getCheckoutRecordForPublishing();

        Assertions.assertTrue(res.isEmpty());
    }

    @Test
    void testGetCheckoutRecordForPublishing_EmptyFailedRecord() {
        int numberOfRecords = 10;
        var mockRecords = Instancio.ofList(Checkout.class).size(numberOfRecords).create();
        Mockito.when(checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(PaymentStatus.FAILED, false))
            .thenReturn(Collections.emptyList());
        Mockito.when(checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(PaymentStatus.FINALIZED, false))
            .thenReturn(mockRecords);

        var res = checkoutStatusPublishWorker.getCheckoutRecordForPublishing();

        Assertions.assertEquals(numberOfRecords, res.size());

        Mockito.verify(checkoutRepository, Mockito.times(2))
            .findTop100ByCheckoutStatusAndEventPublished(Mockito.any(), Mockito.any());
    }

    @Test
    void testGetCheckoutRecordForPublishing_CombineRecords() {
        int numberOfRecords = 10;
        Mockito.when(checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(PaymentStatus.FAILED, false))
            .thenReturn(Instancio.ofList(Checkout.class).size(numberOfRecords).create());
        Mockito.when(checkoutRepository.findTop100ByCheckoutStatusAndEventPublished(PaymentStatus.FINALIZED, false))
            .thenReturn(Instancio.ofList(Checkout.class).size(numberOfRecords).create());

        var res = checkoutStatusPublishWorker.getCheckoutRecordForPublishing();

        Assertions.assertEquals(numberOfRecords * 2, res.size());

        Mockito.verify(checkoutRepository, Mockito.times(2))
            .findTop100ByCheckoutStatusAndEventPublished(Mockito.any(), Mockito.any());
    }

    // integration test will be used for better testing this method
    @SneakyThrows
    @Test
    void testPublish_FailedJsonParsing() {
        var mockCheckouts = Instancio.ofList(Checkout.class).size(1).create();
        Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
            .thenThrow(JsonProcessingException.class);

        Assertions.assertDoesNotThrow(() -> checkoutStatusPublishWorker.publish(mockCheckouts));

        Mockito.verify(kafkaTemplate, Mockito.times(0))
            .send(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @SneakyThrows
    @Test
    void testPublish_FailedKafkaSend(CapturedOutput output) {
        var mockCheckouts = Instancio.ofList(Checkout.class).size(1).create();
        var mockPayload = "dummy";

        Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
            .thenReturn(mockPayload);
        Mockito.when(kafkaTemplate.send(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException()));

        Assertions.assertDoesNotThrow(() -> checkoutStatusPublishWorker.publish(mockCheckouts));

        Mockito.verify(kafkaTemplate, Mockito.times(1))
            .send(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(checkoutRepository, Mockito.times(1))
            .saveAll(Mockito.any());
        Assertions.assertTrue(output.toString().contains("ERROR_KAFKA_SEND for checkout"));
    }

    @SneakyThrows
    @Test
    void testPublish_emptyList(CapturedOutput output) {
        checkoutStatusPublishWorker.publish(Collections.emptyList());

        Assertions.assertFalse(output.toString().contains("Start publishing"));
        Mockito.verify(objectMapper, Mockito.times(0))
            .writeValueAsString(Mockito.any());
    }

    @SneakyThrows
    @Test
    void testPublish_OK(CapturedOutput output) {
        var mockCheckouts = Instancio.ofList(Checkout.class).size(1).create();
        var mockPayload = "dummy";
        var mockRecordData = Instancio.of(RecordMetadata.class).create();
        var mockProducerRecord = Instancio.of(ProducerRecord.class)
            .withTypeParameters(String.class, String.class)
            .create();
        var mockResp = new SendResult<String, String>(mockProducerRecord, mockRecordData);

        Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
            .thenReturn(mockPayload);
        Mockito.when(kafkaTemplate.send(
            MessageBrokerConstant.CHECKOUT_STATUS_TOPIC,
            mockCheckouts.get(0).getOrderId(),
            mockPayload
        )).thenReturn(CompletableFuture.completedFuture(mockResp));


        Assertions.assertDoesNotThrow(() -> checkoutStatusPublishWorker.publish(mockCheckouts));

        Mockito.verify(kafkaTemplate, Mockito.times(1))
            .send(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(checkoutRepository, Mockito.times(1))
            .saveAll(Mockito.any());
        Assertions.assertTrue(output.toString().contains("Start publishing"));
        Assertions.assertTrue(output.toString().contains("Published checkout of order %s"
            .formatted(mockCheckouts.get(0).getOrderId())));
    }

}
