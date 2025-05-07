package db

import (
	"context"
	"database/sql"
	"fmt"
	"log/slog"
	"testing"

	"github.com/TrungTho/saga-playground/constants"
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

	require.Exactly(t, numberOfConcurrent, successCancellation, "Order cancellation is an idempotent action")
	require.Exactly(t, 0, failedCancellation, "Other cancel action should be failed")
}

func TestValidateAndUpdateOrderStatus(t *testing.T) {
	// no existing record
	invalidId := -1 // id which we can make sure it will be forever a invalid id
	id, err := testStore.ValidateAndUpdateOrderStatusTx(context.Background(), invalidId, OrderStatusCreated, OrderStatusCancelled, slog.Attr{})

	require.Equal(t, -1, id, "failed update should always return id of order as -1")
	require.NotNil(t, err, "failed update should always return error")
	require.Contains(t, sql.ErrNoRows.Error(), err.Error(), "error should says about no rows were found")

	// mismatch current status -> update will be aborted
	createdOrder, err := createNewTestOrder("", OrderStatusCreated)
	require.NoError(t, err, "Should be able to create test order")
	id, err = testStore.ValidateAndUpdateOrderStatusTx(context.Background(), int(createdOrder.ID), OrderStatusCancelled, OrderStatusAwaitingFulfillment, slog.Attr{})

	require.Equal(t, -1, id, "failed update should always return id of order as -1")
	require.NotNil(t, err, "failed update should always return error")
	require.Equal(t, constants.INVALID_ACTION, err.Error(), "error should says invalid action")

	expectedStatus1 := OrderStatusPendingPayment
	expectedStatus2 := OrderStatusAwaitingFulfillment
	expectedStatus3 := OrderStatusCancelled

	// update successfully 1
	id, err = testStore.ValidateAndUpdateOrderStatusTx(context.Background(), int(createdOrder.ID), OrderStatusCreated, expectedStatus1, slog.Attr{})
	require.EqualValues(t, createdOrder.ID, id, "successful update should always returns exact updated order id")
	require.Nil(t, err, "successful update should always return nil as error")

	retrievedOrder, err := testStore.GetOrder(context.Background(), int32(id))
	require.NoError(t, err, "Should be able to retrieve order after the first update")
	require.Equal(t, expectedStatus1, retrievedOrder.Status, fmt.Sprintf("first updated status should be %v", expectedStatus1))

	// update successfully 2
	id, err = testStore.ValidateAndUpdateOrderStatusTx(context.Background(), int(createdOrder.ID), expectedStatus1, expectedStatus2, slog.Attr{})
	require.EqualValues(t, createdOrder.ID, id, "successful update should always returns exact updated order id")
	require.Nil(t, err, "successful update should always return nil as error")

	retrievedOrder, err = testStore.GetOrder(context.Background(), int32(id))
	require.NoError(t, err, "Should be able to retrieve order after the second update")
	require.Equal(t, expectedStatus2, retrievedOrder.Status, fmt.Sprintf("second updated status should be %v", expectedStatus2))

	// update successfully 3
	id, err = testStore.ValidateAndUpdateOrderStatusTx(context.Background(), int(createdOrder.ID), expectedStatus2, expectedStatus3, slog.Attr{})
	require.EqualValues(t, createdOrder.ID, id, "successful update should always returns exact updated order id")
	require.Nil(t, err, "successful update should always return nil as error")

	retrievedOrder, err = testStore.GetOrder(context.Background(), int32(id))
	require.NoError(t, err, "Should be able to retrieve order after the third update")
	require.Equal(t, expectedStatus3, retrievedOrder.Status, fmt.Sprintf("third updated status should be %v", expectedStatus3))
}
