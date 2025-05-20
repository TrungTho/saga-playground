package kafkaclient_test

import (
	"bytes"
	"log"
	"log/slog"
	"os"
	"strings"
	"testing"

	"github.com/TrungTho/saga-playground/constants"
	kafkaclient "github.com/TrungTho/saga-playground/kafka"
	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/go-faker/faker/v4"
	"github.com/stretchr/testify/require"
)

var k *kafkaclient.KafkaStore

func setup(t *testing.T) {
	consumer, err := kafkaclient.NewKafkaConsumer(packageConfig)
	require.Nil(t, err, "Error should be nil when create a new consumer")
	k = &kafkaclient.KafkaStore{
		Consumer:        consumer,
		MessageHandlers: map[string]kafkaclient.MessageHandler{},
	}
}

func TestRegisterHandler(t *testing.T) {
	setup(t)

	require.Empty(t, k.MessageHandlers, "Map of handlers should be empty before the test starts")

	tmpFunc := func(msg *kafka.Message) {
		t.Log("I do something not too special")
	}
	mockTopicName := "test-topic"

	res := k.RegisterHandler(mockTopicName, tmpFunc)

	require.Nil(t, res, "Error should be nil in case of successfully registration")

	_, ok := k.MessageHandlers[mockTopicName]

	require.True(t, ok, "Mock handler should be able to be retrieved")

	// register again -> error should be return
	res = k.RegisterHandler(mockTopicName, tmpFunc)

	require.NotNil(t, res, "Error should be returned in case of duplication handler registration")

	require.Equal(t, constants.ERROR_HANDLER_DUPLICATION, res.Error(), "Error content should indicates the duplication")
}

func TestHandle_InvalidTopicName(t *testing.T) {
	setup(t)

	mockTopicName := faker.Word()
	mockMsg := &kafka.Message{
		TopicPartition: kafka.TopicPartition{
			Topic: &mockTopicName,
		},
	}

	// set up log assertion
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	k.Handle(mockMsg)

	require.True(t, strings.Contains(buf.String(), constants.ERROR_HANDLER_NOT_EXIST),
		"Error should indicates handler does not exist")
}

func TestBatchHandle(t *testing.T) {
	setup(t)

	require.Empty(t, k.MessageHandlers, "Map of handlers should be empty before the test starts")

	mockLog := "I do something not too much special"
	tmpFunc := func(msg *kafka.Message) {
		slog.Info(mockLog, slog.String("TOPIC_NAME", *msg.TopicPartition.Topic))
	}
	mockTopicName := "test-topic"

	res := k.RegisterHandler(mockTopicName, tmpFunc)

	require.Nil(t, res, "Error should be nil in case of successfully registration")

	_, ok := k.MessageHandlers[mockTopicName]

	require.True(t, ok, "Mock handler should be able to be retrieved")

	mockMsg := &kafka.Message{
		TopicPartition: kafka.TopicPartition{
			Topic: &mockTopicName,
		},
	}

	mockMap := make(map[string][]*kafka.Message)
	mockMap["1"] = []*kafka.Message{mockMsg}
	mockCount := 1

	k.MessageMap = mockMap
	k.MessageCount = mockCount

	// set up log assertion
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	k.BatchHandle()

	t.Log("this is the buffer", buf.String())

	require.True(t, strings.Contains(buf.String(), mockLog))
	require.True(t, strings.Contains(buf.String(), mockTopicName))
}
