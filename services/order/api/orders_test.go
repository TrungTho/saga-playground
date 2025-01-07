package api

import (
	"database/sql"
	"errors"
	"fmt"
	"math/big"
	"net/http"
	"net/http/httptest"
	"reflect"
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

type eqCreateOrderParamsMatcher struct {
	arg db.CreateOrderParams
}

func (e eqCreateOrderParamsMatcher) Matches(x interface{}) bool {
	arg, ok := x.(db.CreateOrderParams)
	if !ok {
		return false
	}

	e.arg.Amount.Int = arg.Amount.Int
	return reflect.DeepEqual(e.arg, arg)
}

func (e eqCreateOrderParamsMatcher) String() string {
	return fmt.Sprintf("matches arg %v", e.arg)
}

func EqCreateOrderParams(arg db.CreateOrderParams) gomock.Matcher {
	return eqCreateOrderParamsMatcher{arg}
}

func TestCreateOrder(t *testing.T) {
	dummyMsg := "dummy"
	mockOrder := db.Order{
		ID:      int32(util.RandomInt(1, 100)),
		UserID:  faker.UUIDDigit(),
		Status:  db.OrderStatusCreated,
		Amount:  fakeAmount,
		Message: &dummyMsg,
	}

	testcases := []struct {
		testName      string
		mockUserId    string
		buildStubs    func(dbStore *mock_db.MockDBStore)
		checkResponse func(t *testing.T, recorder *httptest.ResponseRecorder)
	}{
		{
			testName:   "OK",
			mockUserId: mockOrder.UserID,
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				args := db.CreateOrderParams{
					UserID: mockOrder.UserID,
					Status: mockOrder.Status,
					Amount: pgtype.Numeric{
						Int:   big.NewInt(1),
						Exp:   -2,
						Valid: true,
					},
				}
				dbStore.EXPECT().CreateOrder(gomock.Any(), EqCreateOrderParams(args)).Times(1).Return(mockOrder, nil)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusOK, recorder.Code)

				var resp RestResponse
				util.ConvertByteToStruct(t, recorder.Body, &resp)
				mpOrder, err := resp.Data.(map[string]interface{})
				require.True(t, err, "Response data conversion should be successfully")

				require.EqualValues(t, mockOrder.ID, mpOrder["id"], "ID should be equal")
				require.Equal(t, mockOrder.UserID, mpOrder["user_id"], "UserId should be equal")
				require.Equal(t, *mockOrder.Message, mpOrder["message"], "Message should be equal")
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
		{
			testName:   "Error DB",
			mockUserId: mockOrder.UserID,
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				args := db.CreateOrderParams{
					UserID: mockOrder.UserID,
					Status: mockOrder.Status,
					Amount: pgtype.Numeric{
						Int:   big.NewInt(1),
						Exp:   -2,
						Valid: true,
					},
				}
				dbStore.EXPECT().CreateOrder(gomock.Any(), EqCreateOrderParams(args)).Times(1).Return(db.Order{}, sql.ErrConnDone)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusInternalServerError, recorder.Code)
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
	dummyMsg := "dummy"
	mockOrder := db.Order{
		ID:      int32(util.RandomInt(1, 100)),
		UserID:  faker.UUIDDigit(),
		Status:  db.OrderStatusCreated,
		Amount:  fakeAmount,
		Message: &dummyMsg,
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

				var resp RestResponse
				util.ConvertByteToStruct(t, recorder.Body, &resp)
				mpOrder, err := resp.Data.(map[string]interface{})
				require.True(t, err, "Response data conversion should be successfully")

				require.EqualValues(t, mockOrder.ID, mpOrder["id"], "ID should be equal")
				require.Equal(t, mockOrder.UserID, mpOrder["user_id"], "UserId should be equal")
				require.Equal(t, *mockOrder.Message, mpOrder["message"], "Message should be equal")
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
			testName:    "DB not found",
			mockOrderId: fmt.Sprintf("%v", mockOrder.ID),
			buildStubs: func(dbStore *mock_db.MockDBStore) {
				dbStore.EXPECT().GetOrder(gomock.Any(), gomock.Eq(mockOrder.ID)).Times(1).Return(db.Order{}, sql.ErrNoRows)
			},
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusNotFound, recorder.Code)
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
				dbStore.EXPECT().CancelOrderTx(gomock.Any(), gomock.Eq(int(mockOrder.ID)), gomock.Any()).Times(1).Return(int(mockOrder.ID), nil)
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
				dbStore.EXPECT().CancelOrderTx(gomock.Any(), gomock.Eq(int(mockOrder.ID)), gomock.Any()).Times(1).Return(-1, errors.New("invalid action"))
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
