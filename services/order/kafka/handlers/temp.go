package handlers

import (
	"log/slog"

	kafkaclient "github.com/TrungTho/saga-playground/kafka"
)

func HandleTmpMessage(p *kafkaclient.MessageHandlerParams) {
	slog.Info("test",
		slog.String("TOPIC", *p.Message.TopicPartition.Topic),
		slog.String("MESSAGE", string(p.Message.Value)),
	)
}
