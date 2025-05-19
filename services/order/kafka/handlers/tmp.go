package handlers

import (
	"log"
	"log/slog"

	kafkaclient "github.com/TrungTho/saga-playground/kafka"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func RegisterTmpHandler(k kafkaclient.KafkaOperations) {
	if err := k.RegisterHandler("haha", handleTmpMessage); err != nil {
		log.Fatalf("Can't register handler for topic haha")
	}

	slog.Info("successfully register message handler for haha topic")
}

func handleTmpMessage(msg *kafka.Message) {
	slog.Info("haha",
		slog.String("TOPIC", *msg.TopicPartition.Topic),
		slog.String("MESSAGE", string(msg.Value)),
	)
}
