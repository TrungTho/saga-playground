package db

import (
	"context"
	"log/slog"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCancelOrderTx(t *testing.T) {
	createdOrder, err := createNewTestOrder("", OrderStatusCreated)

	require.NoError(t, err, "Should be able to create test order")

	idChannel := make(chan int)

	numberOfConcurrent := 20
	for i := 0; i < numberOfConcurrent; i++ {
		go func() {
			orderId, _ := testStore.CancelOrderTx(context.Background(),
				int(createdOrder.ID), slog.Attr{})
			idChannel <- orderId
		}()
	}

	successCancellation := 0
	failedCancellation := 0

	for i := 0; i < numberOfConcurrent; i++ {
		id := <-idChannel
		if id == -1 {
			failedCancellation++
		} else {
			successCancellation++
		}
	}

	require.Exactly(t, 1, successCancellation, "Only 1 cancel action should be successful")
	require.Exactly(t, numberOfConcurrent-1, failedCancellation, "Other cancel action should be failed")
}
