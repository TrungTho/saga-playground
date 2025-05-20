package kafkaclient

import (
	"context"
	"log/slog"
	"time"

	"github.com/TrungTho/saga-playground/util"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

type KafkaOperations interface {
	KafkaConsumerOperations
	// KafkaProducerOperations
	Close()
}

type KafkaConsumerOperations interface {
	RegisterHandler(topicName string, newHandler MessageHandler) error // register functions to handle different message types
	SubscribeTopics(ctx context.Context, topicNames []string) error    // subscribe to multiple topics at the same time & start consuming messages from them
}

// for mocking actual implementation
type KafkaConsumer interface {
	SubscribeTopics(topics []string, rebalanceCb kafka.RebalanceCb) (err error)
	ReadMessage(timeout time.Duration) (*kafka.Message, error)
	CommitMessage(m *kafka.Message) ([]kafka.TopicPartition, error)
	Close() (err error)
}

type KafkaProducer interface {
	// just a placeholder now
}

type KafkaStore struct {
	Consumer        KafkaConsumer
	Producer        KafkaProducer
	MessageHandlers map[string]MessageHandler
	MessageCount    int                         // counting for the below total messages
	MessageMap      map[string][]*kafka.Message // Storage for messages to be batch processed. Our case requires a map for messages of keys.
}

func (k *KafkaStore) Close() {
	k.Consumer.Close()
	// k.p.Close()

	slog.Info("Successfully close consumer and producer")
}

func NewKafkaStore(config util.Config) (KafkaOperations, error) {
	consumer, err := NewKafkaConsumer(config)
	if err != nil {
		return nil, err
	}

	return &KafkaStore{
		Consumer:        consumer,
		Producer:        nil, // not implemented yet
		MessageHandlers: make(map[string]MessageHandler),
		MessageCount:    0, // no message so far
		MessageMap:      make(map[string][]*kafka.Message),
	}, nil
}
