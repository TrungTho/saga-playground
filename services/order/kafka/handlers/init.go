package handlers

import (
	"log/slog"

	kafkaclient "github.com/TrungTho/saga-playground/kafka"
)

func RegisterHandler(k kafkaclient.KafkaOperations, topicName string, handler kafkaclient.MessageHandler) error {
	if err := k.RegisterHandler(topicName, handler); err != nil {
		return err
	}

	slog.Info("Successfully register message handler for topic",
		slog.String("TOPIC_NAME", topicName))

	return nil
}
