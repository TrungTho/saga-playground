package handlers

import (
	"log/slog"

	kafkaclient "github.com/TrungTho/saga-playground/kafka"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func RegisterTmpHandler(k kafkaclient.KafkaOperations) error {
	if err := k.RegisterHandler("haha", handleTmpMessage); err != nil {
		return err
	}

	slog.Info("Successfully register message handler for haha topic")

	return nil
}

func handleTmpMessage(msg *kafka.Message) {
	slog.Info("haha",
		slog.String("TOPIC", *msg.TopicPartition.Topic),
		slog.String("MESSAGE", string(msg.Value)),
	)
}
