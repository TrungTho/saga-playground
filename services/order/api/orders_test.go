package api

import (
	"errors"
	"fmt"
	"math/big"
	"net/http"
	"net/http/httptest"
	"testing"

	mock_db "github.com/TrungTho/saga-playground/db/mock"
	db "github.com/TrungTho/saga-playground/db/sqlc"
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

func TestCreateOrder(t *testing.T) {
	mockOrder := db.Order{
		ID:     int32(util.RandomInt(1, 100)),
		UserID: faker.UUIDDigit(),
		Status: db.OrderStatusCreated,
		Amount: fakeAmount,
	}

	testcases := []struct {
		testName      string
		mockUserId    string
		buildStubs    func(dbStore *mock_db.MockDBStore)
		checkResponse func(t *testing.T, recorder *httptest.ResponseRecorder)
	}{
		{
			testName:   "OK",
			mockUserId: faker.UUIDDigit(),
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().CreateOrder(gomock.Any(), gomock.Any()).Times(1).Return(mockOrder, nil)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusOK, recorder.Code)

				var gotOrder CreateOrderResponse
				util.ConvertByteToStruct(t, recorder.Body, &gotOrder)

				require.Equal(t, mockOrder.ID, gotOrder.ID, "ID should be equal")
				require.Equal(t, mockOrder.UserID, gotOrder.UserID, "UserId should be equal")
				require.Equal(t, mockOrder.Message, gotOrder.Message, "Message should be equal")
			},
		},
		{
			testName: "Missing UserId",
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().CreateOrder(gomock.Any(), gomock.Any()).Times(0).Return(db.Order{}, nil)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusBadRequest, recorder.Code)
			},
		},
	}

	for i := range testcases {
		testcase := testcases[i]

		t.Run(testcase.testName, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			setupTest(ctrl)

			mockReq := CreateOrderRequest{
				UserId: testcase.mockUserId,
			}

			testcase.buildStubs(dbStore)

			restServer, err := NewServer(dbStore)
			require.NoError(t, err, "Can not start mock server", err, testcase.testName)

			recorder := httptest.NewRecorder()
			url := "/orders"
			req, err := http.NewRequest(http.MethodPost, url, util.ConvertStructToByte(t, mockReq))
			require.NoError(t, err, "No error when creating request")

			restServer.router.ServeHTTP(recorder, req)

			testcase.checkResponse(t, recorder)
		})
	}
}

func TestGetOrder(t *testing.T) {
	mockOrder := db.Order{
		ID:     int32(util.RandomInt(1, 100)),
		UserID: faker.UUIDDigit(),
		Status: db.OrderStatusCreated,
		Amount: fakeAmount,
	}

	testcases := []struct {
		testName      string
		mockOrderId   string
		buildStubs    func(dbStore *mock_db.MockDBStore)
		checkResponse func(t *testing.T, recorder *httptest.ResponseRecorder)
	}{
		{
			testName:    "OK",
			mockOrderId: fmt.Sprintf("%v", mockOrder.ID),
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().GetOrder(gomock.Any(), gomock.Eq(mockOrder.ID)).Times(1).Return(mockOrder, nil)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusOK, recorder.Code)

				var gotOrder CreateOrderResponse
				util.ConvertByteToStruct(t, recorder.Body, &gotOrder)

				require.Equal(t, mockOrder.ID, gotOrder.ID, "ID should be equal")
				require.Equal(t, mockOrder.UserID, gotOrder.UserID, "UserId should be equal")
				require.Equal(t, mockOrder.Message, gotOrder.Message, "Message should be equal")
			},
		},
		{
			testName:    "Invalid order id",
			mockOrderId: "gajw",
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().GetOrder(gomock.Any(), gomock.Any()).Times(0).Return(db.Order{}, nil)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusBadRequest, recorder.Code)
			},
		},
	}

	for i := range testcases {
		testcase := testcases[i]

		t.Run(testcase.testName, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			setupTest(ctrl)

			testcase.buildStubs(dbStore)

			restServer, err := NewServer(dbStore)
			require.NoError(t, err, "Can not start mock server", err, testcase.testName)

			recorder := httptest.NewRecorder()
			url := fmt.Sprintf("/orders/%v", testcase.mockOrderId)
			req, err := http.NewRequest(http.MethodGet, url, nil)
			require.NoError(t, err, "No error when creating request")

			restServer.router.ServeHTTP(recorder, req)

			testcase.checkResponse(t, recorder)
		})
	}
}

func TestCancelOrder(t *testing.T) {
	mockOrder := db.Order{
		ID:     int32(util.RandomInt(1, 100)),
		UserID: faker.UUIDDigit(),
		Status: db.OrderStatusCreated,
		Amount: fakeAmount,
	}

	testcases := []struct {
		testName      string
		mockOrderId   string
		buildStubs    func(dbStore *mock_db.MockDBStore)
		checkResponse func(t *testing.T, recorder *httptest.ResponseRecorder)
	}{
		{
			testName:    "OK",
			mockOrderId: fmt.Sprintf("%v", mockOrder.ID),
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().CancelOrderTx(gomock.Any(), gomock.Any()).Times(1).Return(int(mockOrder.ID), nil)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusNoContent, recorder.Code)
			},
		},
		{
			testName:    "Invalid order id",
			mockOrderId: "gajw",
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().GetOrder(gomock.Any(), gomock.Any()).Times(0).Return(db.Order{}, nil)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusBadRequest, recorder.Code)
			},
		},
		{
			testName:    "Failed transaction",
			mockOrderId: fmt.Sprintf("%v", mockOrder.ID),
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().CancelOrderTx(gomock.Any(), gomock.Any()).Times(1).Return(-1, errors.New("invalid action"))
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusBadRequest, recorder.Code)
			},
		},
	}

	for i := range testcases {
		testcase := testcases[i]

		t.Run(testcase.testName, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			setupTest(ctrl)

			testcase.buildStubs(dbStore)

			restServer, err := NewServer(dbStore)
			require.NoError(t, err, "Can not start mock server", err, testcase.testName)

			recorder := httptest.NewRecorder()
			url := fmt.Sprintf("/orders/%v", testcase.mockOrderId)
			req, err := http.NewRequest(http.MethodDelete, url, nil)
			require.NoError(t, err, "No error when creating request")

			restServer.router.ServeHTTP(recorder, req)

			testcase.checkResponse(t, recorder)
		})
	}
}
