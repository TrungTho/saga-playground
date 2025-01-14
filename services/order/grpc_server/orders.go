package grpc_server

import (
	"context"
	"log/slog"

	"github.com/TrungTho/saga-playground/constants"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/pb"
)

func (server GRPCServer) SwitchOrderToPendingPayment(ctx context.Context, req *pb.SwitchToPendingPaymentRequest) (*pb.SwitchToPendingPaymentResponse, error) {
	logFields := slog.Group("rpc",
		slog.String("method", "SwitchOrderToPendingPayment"),
		slog.Any("request", req),
	)

	// call db transaction for check and update status to pending payment
	orderId, err := server.dbStore.ValidateAndUpdateOrderStatusTx(ctx, int(req.Id), db.OrderStatusCreated, db.OrderStatusPendingPayment, logFields)
	// return successful or failed status
	if err != nil {
		slog.ErrorContext(ctx, constants.ERROR_ORDER_RPC_START_PAYMENT, logFields, slog.Any("error", err))

		return nil, err
	}

	slog.InfoContext(ctx, constants.ORDER_STATUS_CHANGED, logFields, slog.String(
		"new_status", string(db.OrderStatusPendingPayment),
	))

	return &pb.SwitchToPendingPaymentResponse{
		Order: &pb.Order{
			Id:     int32(orderId),
			Status: string(db.OrderStatusPendingPayment),
		},
	}, nil
}
