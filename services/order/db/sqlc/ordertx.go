package db

import (
	"context"
	"errors"
	"log/slog"

	"github.com/TrungTho/saga-playground/constants"
)

// we can only cancel an newly created order
func (store *SQLStore) CancelOrderTx(ctx context.Context, id int, logFields slog.Attr) (orderId int, err error) {
	return store.ValidateAndUpdateOrderStatusTx(ctx, id, OrderStatusCreated, OrderStatusCancelled, logFields)
}

// this function will help to update status of order,
// but it will check the current status of that order first to make sure it's in accepted status before making the change
// Parameters:
//   - id: id of the order
//   - expectedCurrentStatus: the current status of order which we are expecting it is (for state machine check)
//   - newStatus: the new status of order that we would like to update it to (if the above status is correct)
//
// Returns:
//   - id: id of the updated order
//   - err: error (in case expectedCurrentStatus does not match the actual status of order in db record, or transaction error, etc.)
func (store *SQLStore) ValidateAndUpdateOrderStatusTx(ctx context.Context, id int, expectedCurrentStatus OrderStatus, newStatus OrderStatus, logFields slog.Attr) (orderId int, err error) {
	err = store.execTx(ctx, func(q *Queries) error {
		order, err := q.GetOrder(ctx, int32(id)) // NOTICE: q, the querier which was init inside the transaction, NOT THE store.Querier.GetOrder(), otherwise the go-routine will be blocked forever
		if err != nil {
			return err
		}

		// already updated, no further action needed
		if order.Status == newStatus {
			return nil
		}

		// update needed -> validate if the current status is correct
		if order.Status != expectedCurrentStatus {
			slog.ErrorContext(ctx,
				constants.ERROR_ORDER_INVALID_STATUS, logFields,
				slog.String("current_status", string(order.Status)),
				slog.String("expected_current_status", string(expectedCurrentStatus)),
			)
			return errors.New(constants.INVALID_ACTION)
		}

		_, err = q.UpdateOrderStatus(ctx,
			UpdateOrderStatusParams{ID: order.ID, Status: newStatus})
		if err != nil {
			slog.ErrorContext(ctx, constants.ERROR_ORDER_UPDATE_FAILED,
				logFields,
				slog.String("current_status", string(order.Status)),
				slog.String("new_status", string(newStatus)),
				slog.String("expected_current_status", string(expectedCurrentStatus)),
				slog.Any("error", err),
			)
			return err
		}

		return nil
	})
	if err != nil {
		return -1, err
	}

	return int(id), nil
}
