package handlers

import (
	"bytes"
	"errors"
	"log"
	"os"
	"strings"
	"testing"

	"github.com/TrungTho/saga-playground/constants"
	mock_kafkaclient "github.com/TrungTho/saga-playground/kafka/mock"
	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

func TestRegisterHandler_Failed(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	mockKafkaStore := mock_kafkaclient.NewMockKafkaOperations(ctrl)

	mockKafkaStore.EXPECT().RegisterHandler(gomock.Any(), gomock.Any()).Return(errors.New(constants.ERROR_HANDLER_DUPLICATION))

	// set up log assertion
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	err := RegisterTmpHandler(mockKafkaStore)

	require.NotNil(t, err, "Error should not be nil")
	require.Equal(t, constants.ERROR_HANDLER_DUPLICATION, err.Error(), "Error should reflect the internal exception")
	require.False(t, strings.Contains(buf.String(), "Successfully"))
}

func TestRegisterHandler_OK(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	mockKafkaStore := mock_kafkaclient.NewMockKafkaOperations(ctrl)

	mockKafkaStore.EXPECT().RegisterHandler(gomock.Any(), gomock.Any()).Return(nil)

	// set up log assertion
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	err := RegisterTmpHandler(mockKafkaStore)

	require.Nil(t, err, "Error should be nil")
	require.True(t, strings.Contains(buf.String(), "Successfully register message handler for haha topic"))
}

func TestHandlerTmpMessage(t *testing.T) {
	// set up log assertion
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	mockTopicName := "haha"

	mockMsg := &kafka.Message{
		TopicPartition: kafka.TopicPartition{
			Topic: &mockTopicName,
		},
	}

	handleTmpMessage(mockMsg)

	require.True(t, strings.Contains(buf.String(), "haha"))
}
