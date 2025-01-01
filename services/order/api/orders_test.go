package api

import (
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
	dbQueries  *mock_db.MockQuerier
)

func setupTest(ctrl *gomock.Controller) {
	randFloatValue := float64(util.RandomInt(1, 100)) + util.RandomFloat(1)
	fakeAmount = pgtype.Numeric{
		Int:   big.NewInt(int64(randFloatValue * 100)),
		Exp:   -2,
		Valid: true,
	}

	dbQueries = mock_db.NewMockQuerier(ctrl)
}

func TestCreateAccount(t *testing.T) {
	mockOrder := db.Order{
		ID:     int32(util.RandomInt(1, 100)),
		UserID: faker.UUIDDigit(),
		Status: db.OrderStatusCreated,
		Amount: fakeAmount,
	}

	testcases := []struct {
		testName      string
		mockUserId    string
		buildStubs    func(dbQuerier *mock_db.MockQuerier)
		checkResponse func(t *testing.T, recorder *httptest.ResponseRecorder)
	}{
		{
			testName:   "OK",
			mockUserId: faker.UUIDDigit(),
			buildStubs: func(dbQuerier *mock_db.MockQuerier) {
				dbQuerier.EXPECT().CreateOrder(gomock.Any(), gomock.Any()).Times(1).Return(mockOrder, nil)
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
			buildStubs: func(dbQuerier *mock_db.MockQuerier) {
				dbQuerier.EXPECT().CreateOrder(gomock.Any(), gomock.Any()).Times(0).Return(db.Order{}, nil)
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

			testcase.buildStubs(dbQueries)

			restServer, err := NewServer(dbQueries)
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
