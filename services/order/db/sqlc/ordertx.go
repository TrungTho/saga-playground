package db

import (
	"context"
	"errors"
	"log/slog"

	"github.com/TrungTho/saga-playground/constants"
)

func (store *SQLStore) CancelOrderTx(ctx context.Context, id int, logFields slog.Attr) (orderId int, err error) {
	err = store.execTx(ctx, func(q *Queries) error {
		order, err := q.GetOrder(ctx, int32(id)) // NOTICE: q, the querier which was init inside the transaction, not the store.Querier.GetOrder(), otherwise the go-routine will be blocked forever
		if err != nil {
			return err
		}

		if order.Status != OrderStatusCreated {
			slog.ErrorContext(ctx,
				constants.ERROR_ORDER_CANCEL_INVALID_STATUS, logFields,
				slog.String("current_status", string(order.Status)),
				slog.String("accepted_status", string(OrderStatusCreated)),
			)
			return errors.New(constants.INVALID_ACTION)
		}

		_, err = q.UpdateOrderStatus(ctx,
			UpdateOrderStatusParams{ID: order.ID, Status: OrderStatusCancelled})
		if err != nil {
			slog.ErrorContext(ctx, constants.ERROR_ORDER_UPDATE_FAILED,
				logFields,
				slog.String("current_status", string(order.Status)),
				slog.Any("error", err),
			)
			return err
		}

		return nil
	})
	if err != nil {
		return -1, err
	}

	return id, nil
}
