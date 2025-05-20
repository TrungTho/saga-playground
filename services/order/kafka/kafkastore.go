package kafkaclient

import (
	"context"
	"log/slog"

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

// type KafkaProducerOperations interface{}

type KafkaStore struct {
	c               *kafka.Consumer
	p               *kafka.Producer
	messageHandlers map[string]MessageHandler
	messageCount    int                         // counting for the below total messages
	messageMap      map[string][]*kafka.Message // Storage for messages to be batch processed. Our case requires a map for messages of keys.
}

func (k *KafkaStore) Close() {
	k.c.Close()
	// k.p.Close()

	slog.Info("Successfully close consumer and producer")
}

func NewKafkaStore(config util.Config) (KafkaOperations, error) {
	consumer, err := NewConsumer(config)
	if err != nil {
		return nil, err
	}

	return &KafkaStore{
		c:               consumer,
		p:               nil, // not implemented yet
		messageHandlers: make(map[string]MessageHandler),
		messageCount:    0, // no message so far
		messageMap:      make(map[string][]*kafka.Message),
	}, nil
}
