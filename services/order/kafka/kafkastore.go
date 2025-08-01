package kafkaclient

import (
	"context"
	"log/slog"
	"time"

	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/util"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
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
	dbStore         db.DBStore
}

// this struct is used to encapsulate all needed data when handle a kafka message in order not to flood the number of params of the handler functions
type MessageHandlerParams struct {
	Message *kafka.Message
	DBStore db.DBStore
	// more data can be added here
}

func (k *KafkaStore) Close() {
	k.Consumer.Close()
	// k.p.Close()

	slog.Info("Successfully close consumer and producer")
}

func NewKafkaStore(config util.Config, dbStore db.DBStore) (KafkaOperations, error) {
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
		dbStore:         dbStore,
	}, nil
}
