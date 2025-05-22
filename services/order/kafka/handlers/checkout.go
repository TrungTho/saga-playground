package handlers

import (
	"context"
	"encoding/json"
	"log/slog"
	"strconv"

	"github.com/TrungTho/saga-playground/constants"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	kafkaclient "github.com/TrungTho/saga-playground/kafka"
)

type CheckoutMessage struct {
	OrderId string
	Status  string
}

func HandleCheckoutUpdateMessage(p *kafkaclient.MessageHandlerParams) {
	slog.Info("Handle checkout status event",
		slog.String("TOPIC", *p.Message.TopicPartition.Topic),
		slog.String("MESSAGE", string(p.Message.Value)),
	)

	var msg CheckoutMessage

	err := json.Unmarshal(p.Message.Value, &msg)
	if err != nil {
		slog.Error(constants.ERROR_CHECKOUT_MESSAGE_PARSING,
			slog.String("MESSAGE", string(p.Message.Value)),
			slog.Any("ERROR", err))
		return
	}

	orderId, err := strconv.Atoi(msg.OrderId)
	if err != nil {
		slog.Error(constants.ERROR_CHECKOUT_MESSAGE_PARSING,
			slog.String("ORDER_ID", msg.OrderId),
			slog.Any("ERROR", err))
		return
	}

	// retrieve order from db
	order, err := p.DBStore.GetOrder(context.Background(), int32(orderId))
	if err != nil {
		slog.Error(constants.ERROR_INTERNAL,
			slog.Any("ERROR", err))
		return
	}

	// decide new status
	newStatus := order.Status
	switch msg.Status {
	case constants.CHECKOUT_STATUS_FINALIZED:
		newStatus = db.OrderStatusAwaitingFulfillment
	case constants.CHECKOUT_STATUS_FAILED:
		newStatus = db.OrderStatusFailed
	default:
		slog.Error(constants.INVALID_ACTION,
			slog.Any("MESSAGE", msg))
	}

	if newStatus == order.Status {
		slog.Info("ORDER IS UP TO DATE")
		return // already up to date -> no need for DB interaction
	}

	// update db
	p.DBStore.UpdateOrderStatus(context.Background(), db.UpdateOrderStatusParams{
		ID:     int32(orderId),
		Status: newStatus,
	})
}
