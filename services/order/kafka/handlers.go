package kafkaclient

import (
	"errors"
	"log/slog"

	"github.com/TrungTho/saga-playground/constants"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

type MessageHandler func(event *kafka.Message)

func (k *KafkaStore) RegisterHandler(topicName string, newHandler MessageHandler) error {
	// check if topic was already registered with handler before
	_, ok := k.messageHandlers[topicName]
	if ok {
		return errors.New(constants.ERROR_HANDLER_DUPLICATION)
	}

	// register new handler for topic
	k.messageHandlers[topicName] = newHandler
	return nil
}

func (k *KafkaStore) BatchHandle(messageMap *map[string][]*kafka.Message, eventCount *int) {
	slog.Info("Processing new message batch",
		slog.Int("batch count", *eventCount))
	for _, messages := range *messageMap {
		for _, msg := range messages {
			k.Handle(msg)

			// commit offset
			k.c.CommitMessage(msg)
		}
	}

	// reset counter and batch storage
	*messageMap = make(map[string][]*kafka.Message)
	*eventCount = 0
}

func (k *KafkaStore) Handle(msg *kafka.Message) {
	// check if handler exists to handle this type of message

	handler, ok := k.messageHandlers[*msg.TopicPartition.Topic]
	if !ok {
		slog.Error(constants.ERROR_HANDLER_NOT_EXIST,
			slog.String("TOPIC_NAME", *msg.TopicPartition.Topic))
		return
	}

	// if handler does exist -> invoke to handle the message
	handler(msg)
}
