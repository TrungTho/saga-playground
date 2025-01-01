package api

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/TrungTho/saga-playground/util"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

func TestHealthCheck(t *testing.T) {
	testcases := []struct {
		testName      string
		checkResponse func(t *testing.T, recorder *httptest.ResponseRecorder)
	}{
		{
			testName: "OK",
			checkResponse: func(t *testing.T, recorder *httptest.ResponseRecorder) {
				require.Equal(t, http.StatusOK, recorder.Code)

				var gotValue string
				util.ConvertByteToStruct(t, recorder.Body, &gotValue)

				require.Equal(t, "pong", gotValue, "pong should be received")
			},
		},
	}

	for i := range testcases {
		testcase := testcases[i]

		t.Run(testcase.testName, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			restServer, err := NewServer(dbQueries)
			require.NoError(t, err, "Can not start mock server", err, testcase.testName)

			recorder := httptest.NewRecorder()
			url := "/ping"
			req, err := http.NewRequest(http.MethodGet, url, nil)
			require.NoError(t, err, "No error when creating request")

			restServer.router.ServeHTTP(recorder, req)

			testcase.checkResponse(t, recorder)
		})
	}
}
