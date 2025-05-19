package kafkaclient

import (
	"errors"
	"log/slog"

	"github.com/TrungTho/saga-playground/constants"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

type MessageHandler func(event *kafka.Message)

var handlers = make(map[string]MessageHandler)

func RegisterHandler(topicName string, newHandler MessageHandler) error {
	// check if topic was already registered with handler before
	_, ok := handlers[topicName]
	if ok {
		return errors.New(constants.ERROR_HANDLER_DUPLICATION)
	}

	// register new handler for topic
	handlers[topicName] = newHandler
	return nil
}

func BatchHandle(messageMap *map[string][]*kafka.Message, eventCount *int) {
	slog.Info("Processing new message batch",
		slog.Int("batch count", *eventCount))
	for _, messages := range *messageMap {
		for _, msg := range messages {
			Handle(msg)
		}
	}
}

func Handle(msg *kafka.Message) {
	// check if handler exists to handle this type of message

	handler, ok := handlers[*msg.TopicPartition.Topic]
	if !ok {
		slog.Error(constants.ERROR_HANDLER_NOT_EXIST,
			slog.String("TOPIC_NAME", *msg.TopicPartition.Topic))
		return
	}

	// if handler does exist -> invoke to handle the message
	handler(msg)
}
