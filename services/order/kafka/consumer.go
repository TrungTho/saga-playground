package kafkaclient

import (
	"context"
	"fmt"
	"log"
	"log/slog"
	"time"

	"github.com/TrungTho/saga-playground/constants"
	"github.com/TrungTho/saga-playground/util"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
)

func NewKafkaConsumer(config util.Config) (*kafka.Consumer, error) {
	c, err := kafka.NewConsumer(&kafka.ConfigMap{
		// User-specific properties that you must set
		"bootstrap.servers": fmt.Sprintf("%s:%s", config.KAFKA_BOOTSTRAP_HOST, config.KAFKA_BOOTSTRAP_PORT),

		// Fixed properties
		"group.id":           constants.CONSUMER_GROUP_ID,
		"auto.offset.reset":  constants.AUTO_OFFSET_RESET_MODE,
		"enable.auto.commit": false, // we want to handle event in batches
	})
	if err != nil {
		return nil, err
	}

	return c, nil
}

func (k *KafkaStore) SubscribeTopics(ctx context.Context, topicNames []string) error {
	err := k.Consumer.SubscribeTopics(topicNames, nil)
	if err != nil {
		slog.Error(constants.ERROR_CONSUMER_INITIALIZATION,
			slog.Any("error", err))
		return err
	}

	slog.Info("Successfully subscribe to topics:")
	for _, name := range topicNames {
		log.Println(name)
	}

	// Process messages
	run := true

	for run {
		select {
		case <-ctx.Done():
			slog.InfoContext(ctx, "TERMINATING LISTENERS")

			// process last batch here
			if k.MessageCount > 0 {
				slog.Info("Processing last batch before terminating listeners")
				k.BatchHandle()
			}

			run = false
		default:
			ev, err := k.Consumer.ReadMessage(100 * time.Millisecond)
			if err != nil {
				if key, ok := err.(kafka.Error); ok && key.Code() == kafka.ErrTimedOut {
					// In case of a timeout, do not wait reaching the BATCH_SIZE. Process stored messages.
					if k.MessageCount > 0 {
						k.BatchHandle()
					}
				}

				// Errors are informational and automatically handled by the consumer
				continue
			}

			// add message to batch storage
			k.MessageMap[string(ev.Key)] = append(k.MessageMap[string(ev.Key)], ev)
			k.MessageCount++

			if k.MessageCount%constants.BATCH_SIZE == 0 {
				k.BatchHandle()
			}
		}
	}

	return nil
}
