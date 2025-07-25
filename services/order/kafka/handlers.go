package kafkaclient

import (
	"errors"
	"log/slog"

	"github.com/TrungTho/saga-playground/constants"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
)

type MessageHandler func(args *MessageHandlerParams)

func (k *KafkaStore) RegisterHandler(topicName string, newHandler MessageHandler) error {
	// check if topic was already registered with handler before
	_, ok := k.MessageHandlers[topicName]
	if ok {
		return errors.New(constants.ERROR_HANDLER_DUPLICATION)
	}

	// register new handler for topic
	k.MessageHandlers[topicName] = newHandler
	return nil
}

func (k *KafkaStore) BatchHandle() {
	slog.Info("Processing new message batch",
		slog.Int("batch count", int(k.MessageCount)))
	for _, messages := range k.MessageMap {
		for _, msg := range messages {
			k.Handle(msg)

			// commit offset
			k.Consumer.CommitMessage(msg)
		}
	}

	// reset counter and batch storage
	k.MessageMap = make(map[string][]*kafka.Message)
	k.MessageCount = 0
}

func (k *KafkaStore) Handle(msg *kafka.Message) {
	// check if handler exists to handle this type of message

	handler, ok := k.MessageHandlers[*msg.TopicPartition.Topic]
	if !ok {
		slog.Error(constants.ERROR_HANDLER_NOT_EXIST,
			slog.String("TOPIC_NAME", *msg.TopicPartition.Topic))
		return
	}

	// if handler does exist -> invoke to handle the message
	handler(&MessageHandlerParams{
		Message: msg,
		DBStore: k.dbStore,
	})
}
