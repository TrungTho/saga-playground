package grpc_server

import (
	"context"

	"github.com/TrungTho/saga-playground/pb"
)

func (server GRPCServer) SwitchOrderToPendingPayment(ctx context.Context, req *pb.SwitchToPendingPaymentRequest) (*pb.SwitchToPendingPaymentResponse, error) {
	// call db transaction for check and update status to pending payment

	// return successful or failed status

	return nil, nil
}
