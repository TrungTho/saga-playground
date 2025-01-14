package grpc_server

import (
	"context"
	"errors"
	"fmt"
	"math/big"
	"testing"

	"github.com/TrungTho/saga-playground/constants"
	mock_db "github.com/TrungTho/saga-playground/db/mock"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/pb"
	"github.com/TrungTho/saga-playground/util"
	"github.com/go-faker/faker/v4"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

var (
	fakeAmount pgtype.Numeric
	dbStore    *mock_db.MockDBStore
)

func setupTest(ctrl *gomock.Controller) {
	randFloatValue := float64(util.RandomInt(1, 100)) + util.RandomFloat(1)
	fakeAmount = pgtype.Numeric{
		Int:   big.NewInt(int64(randFloatValue * 100)),
		Exp:   -2,
		Valid: true,
	}

	dbStore = mock_db.NewMockDBStore(ctrl)
}

func TestCancelOrder(t *testing.T) {
	mockOrder := db.Order{
		ID:     int32(util.RandomInt(1, 100)),
		UserID: faker.UUIDDigit(),
		Status: db.OrderStatusCreated,
		Amount: fakeAmount,
	}

	testCases := []struct {
		testName      string
		buildStubs    func(dbStore *mock_db.MockDBStore)
		req           *pb.SwitchToPendingPaymentRequest
		checkResponse func(t *testing.T, resp *pb.SwitchToPendingPaymentResponse, err error)
	}{
		{
			testName: "OK",
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().ValidateAndUpdateOrderStatusTx(gomock.Any(), int(mockOrder.ID), db.OrderStatusCreated, db.OrderStatusPendingPayment, gomock.Any()).Times(1).Return(int(mockOrder.ID), nil)
			},
			req: &pb.SwitchToPendingPaymentRequest{
				Id: mockOrder.ID,
			},
			checkResponse: func(t *testing.T, resp *pb.SwitchToPendingPaymentResponse, err error) {
				require.NoError(t, err, "Successful call should always return nil error")
				require.Equal(t, mockOrder.ID, resp.Order.Id, "ID should be equal")
				require.Equal(t, string(db.OrderStatusPendingPayment), resp.Order.Status, fmt.Sprintf("Status should be %v", db.OrderStatusPendingPayment))
			},
		},
		{
			testName: "InternalError",
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().ValidateAndUpdateOrderStatusTx(gomock.Any(), int(mockOrder.ID), db.OrderStatusCreated, db.OrderStatusPendingPayment, gomock.Any()).Times(1).Return(-1, errors.New(constants.INVALID_ACTION))
			},
			req: &pb.SwitchToPendingPaymentRequest{
				Id: mockOrder.ID,
			},
			checkResponse: func(t *testing.T, resp *pb.SwitchToPendingPaymentResponse, err error) {
				require.NotNil(t, err, "Failed call should always return error")
				require.Nil(t, resp, "Response should be nil")
				require.Equal(t, constants.INVALID_ACTION, err.Error())
			},
		},
	}

	for i := range testCases {
		tc := testCases[i]

		t.Run(tc.testName, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			setupTest(ctrl)

			tc.buildStubs(dbStore)

			grpcServer, err := NewServer(dbStore)
			require.NoError(t, err, "Can not start mock grpc server", err, tc.testName)

			res, err := grpcServer.SwitchOrderToPendingPayment(context.Background(), tc.req)
			tc.checkResponse(t, res, err)
		})
	}
}
