package kafkaclient

import (
	"context"

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
	SubscribeTopics(ctx context.Context, topicNames []string)          // subscribe to multiple topics at the same time & start consuming messages from them
}

// type KafkaProducerOperations interface{}

type KafkaStore struct {
	c               *kafka.Consumer
	p               *kafka.Producer
	messageHandlers map[string]MessageHandler
}

func (k *KafkaStore) Close() {
	k.c.Close()
	// k.p.Close()
}

func NewKafkaStore(config util.Config) KafkaOperations {
	return &KafkaStore{
		c:               NewConsumer(config),
		p:               nil, // not implemented yet
		messageHandlers: make(map[string]MessageHandler),
	}
}
