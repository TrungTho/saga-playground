package handlers

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"strings"
	"testing"

	"github.com/TrungTho/saga-playground/constants"
	mock_db "github.com/TrungTho/saga-playground/db/mock"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	kafkaclient "github.com/TrungTho/saga-playground/kafka"
	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

var mockTopicName = "test-topic"

func TestHandleCheckoutUpdateMessage(t *testing.T) {
	testcases := []struct {
		testName   string
		buildStubs func(ctrl *gomock.Controller) *kafkaclient.MessageHandlerParams
		assertion  func(t *testing.T, bugLog string)
	}{
		{
			testName: "failed to parse message",
			buildStubs: func(ctrl *gomock.Controller) *kafkaclient.MessageHandlerParams {
				return &kafkaclient.MessageHandlerParams{
					Message: &kafka.Message{
						TopicPartition: kafka.TopicPartition{
							Topic: &mockTopicName,
						},
						Value: []byte{},
					},
				}
			},
			assertion: func(t *testing.T, bugLog string) {
				require.True(t, strings.Contains(bugLog, constants.ERROR_CHECKOUT_MESSAGE_PARSING))
			},
		},
		{
			testName: "failed to parse order id",
			buildStubs: func(ctrl *gomock.Controller) *kafkaclient.MessageHandlerParams {
				mockMsg := CheckoutMessage{
					OrderId: "abc",
					Status:  constants.CHECKOUT_STATUS_FAILED,
				}

				msgBytes, _ := json.Marshal(mockMsg)
				return &kafkaclient.MessageHandlerParams{
					Message: &kafka.Message{
						TopicPartition: kafka.TopicPartition{
							Topic: &mockTopicName,
						},
						Value: msgBytes,
					},
				}
			},
			assertion: func(t *testing.T, bugLog string) {
				require.True(t, strings.Contains(bugLog, constants.ERROR_CHECKOUT_MESSAGE_PARSING))
				require.True(t, strings.Contains(bugLog, "ORDER_ID"))
			},
		},
		{
			testName: "order not found in db",
			buildStubs: func(ctrl *gomock.Controller) *kafkaclient.MessageHandlerParams {
				mockMsg := CheckoutMessage{
					OrderId: "123",
					Status:  constants.CHECKOUT_STATUS_FAILED,
				}

				msgBytes, _ := json.Marshal(mockMsg)

				mockDbStore := mock_db.NewMockDBStore(ctrl)
				mockDbStore.EXPECT().GetOrder(gomock.Any(), gomock.Any()).Return(db.Order{}, sql.ErrNoRows)

				mockDbStore.EXPECT().UpdateOrderStatus(gomock.Any(), gomock.Any()).Times(0)

				return &kafkaclient.MessageHandlerParams{
					Message: &kafka.Message{
						TopicPartition: kafka.TopicPartition{
							Topic: &mockTopicName,
						},
						Value: msgBytes,
					},
					DBStore: mockDbStore,
				}
			},
			assertion: func(t *testing.T, bugLog string) {
				require.False(t, strings.Contains(bugLog, constants.ERROR_CHECKOUT_MESSAGE_PARSING))
				require.False(t, strings.Contains(bugLog, "ORDER_ID"))
				require.True(t, strings.Contains(bugLog, constants.ERROR_INTERNAL))
				require.True(t, strings.Contains(bugLog, sql.ErrNoRows.Error()))
			},
		},
		{
			testName: "invalid checkout status",
			buildStubs: func(ctrl *gomock.Controller) *kafkaclient.MessageHandlerParams {
				mockMsg := CheckoutMessage{
					OrderId: "123",
					Status:  "dummy status",
				}

				msgBytes, _ := json.Marshal(mockMsg)

				mockDbStore := mock_db.NewMockDBStore(ctrl)
				mockDbStore.EXPECT().GetOrder(gomock.Any(), gomock.Any()).Return(db.Order{
					ID: 123,
				}, nil)

				mockDbStore.EXPECT().UpdateOrderStatus(gomock.Any(), gomock.Any()).Times(0)

				return &kafkaclient.MessageHandlerParams{
					Message: &kafka.Message{
						TopicPartition: kafka.TopicPartition{
							Topic: &mockTopicName,
						},
						Value: msgBytes,
					},
					DBStore: mockDbStore,
				}
			},
			assertion: func(t *testing.T, bugLog string) {
				require.False(t, strings.Contains(bugLog, constants.ERROR_CHECKOUT_MESSAGE_PARSING))
				require.False(t, strings.Contains(bugLog, "ORDER_ID"))
				require.False(t, strings.Contains(bugLog, constants.ERROR_INTERNAL))
				require.False(t, strings.Contains(bugLog, sql.ErrNoRows.Error()))
				require.True(t, strings.Contains(bugLog, "ORDER IS UP TO DATE"))
			},
		},
		{
			testName: "update order status to failed",
			buildStubs: func(ctrl *gomock.Controller) *kafkaclient.MessageHandlerParams {
				mockOrderId := 123
				mockMsg := CheckoutMessage{
					OrderId: fmt.Sprintf("%d", mockOrderId),
					Status:  constants.CHECKOUT_STATUS_FAILED,
				}

				msgBytes, _ := json.Marshal(mockMsg)

				mockDbStore := mock_db.NewMockDBStore(ctrl)
				mockDbStore.EXPECT().GetOrder(gomock.Any(), gomock.Any()).Return(db.Order{
					ID: int32(mockOrderId),
				}, nil)

				mockDbStore.EXPECT().UpdateOrderStatus(gomock.Any(), gomock.Any()).Times(1)

				return &kafkaclient.MessageHandlerParams{
					Message: &kafka.Message{
						TopicPartition: kafka.TopicPartition{
							Topic: &mockTopicName,
						},
						Value: msgBytes,
					},
					DBStore: mockDbStore,
				}
			},
			assertion: func(t *testing.T, bugLog string) {
				require.False(t, strings.Contains(bugLog, constants.ERROR_CHECKOUT_MESSAGE_PARSING))
				require.False(t, strings.Contains(bugLog, "ORDER_ID"))
				require.False(t, strings.Contains(bugLog, constants.ERROR_INTERNAL))
				require.False(t, strings.Contains(bugLog, sql.ErrNoRows.Error()))
				require.False(t, strings.Contains(bugLog, "ORDER IS UP TO DATE"))
			},
		},
		{
			testName: "update order status to awaitingFulfillment",
			buildStubs: func(ctrl *gomock.Controller) *kafkaclient.MessageHandlerParams {
				mockOrderId := 123
				mockMsg := CheckoutMessage{
					OrderId: fmt.Sprintf("%d", mockOrderId),
					Status:  constants.CHECKOUT_STATUS_FINALIZED,
				}

				msgBytes, _ := json.Marshal(mockMsg)

				mockDbStore := mock_db.NewMockDBStore(ctrl)
				mockDbStore.EXPECT().GetOrder(gomock.Any(), gomock.Any()).Return(db.Order{
					ID: int32(mockOrderId),
				}, nil)

				mockDbStore.EXPECT().UpdateOrderStatus(gomock.Any(), gomock.Any()).Times(1)

				return &kafkaclient.MessageHandlerParams{
					Message: &kafka.Message{
						TopicPartition: kafka.TopicPartition{
							Topic: &mockTopicName,
						},
						Value: msgBytes,
					},
					DBStore: mockDbStore,
				}
			},
			assertion: func(t *testing.T, bugLog string) {
				require.False(t, strings.Contains(bugLog, constants.ERROR_CHECKOUT_MESSAGE_PARSING))
				require.False(t, strings.Contains(bugLog, "ORDER_ID"))
				require.False(t, strings.Contains(bugLog, constants.ERROR_INTERNAL))
				require.False(t, strings.Contains(bugLog, sql.ErrNoRows.Error()))
				require.False(t, strings.Contains(bugLog, "ORDER IS UP TO DATE"))
			},
		},
	}

	for _, tt := range testcases {
		t.Run(tt.testName, func(t *testing.T) {
			// set up log assertion
			var buf bytes.Buffer
			log.SetOutput(&buf)
			defer func() {
				log.SetOutput(os.Stderr)
			}()

			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			// build stub
			params := tt.buildStubs(ctrl)

			// logic
			HandleCheckoutUpdateMessage(params)

			// assertion
			tt.assertion(t, buf.String())
		})
	}
}
