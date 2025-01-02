package db

import (
	"context"
	"errors"

	log "github.com/sirupsen/logrus"
)

func (store *SQLStore) CancelOrderTx(ctx context.Context, id int) (orderId int, err error) {
	err = store.execTx(ctx, func(q *Queries) error {
		order, err := q.GetOrder(ctx, int32(id)) // NOTICE: q, the querier which was init inside the transaction, not the store.Querier.GetOrder(), otherwise the go-routine will be blocked forever
		if err != nil {
			return err
		}

		if order.Status != OrderStatusCreated {
			log.Info("ERROR User try to cancel an invalid order", order.ID, order.Status)
			return errors.New("invalid action")
		}

		_, err = q.UpdateOrderStatus(ctx,
			UpdateOrderStatusParams{ID: order.ID, Status: OrderStatusCancelled})
		if err != nil {
			return err
		}

		return nil
	})
	log.Info("return from CancelOrderTx with err", err)
	if err != nil {
		return -1, err
	}

	return id, nil
}
