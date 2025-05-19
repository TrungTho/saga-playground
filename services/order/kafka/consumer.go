package kafkaclient

import (
	"context"
	"fmt"
	"log"
	"log/slog"
	"time"

	"github.com/TrungTho/saga-playground/constants"
	"github.com/TrungTho/saga-playground/util"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func NewConsumer(config util.Config) (*kafka.Consumer, error) {
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
	err := k.c.SubscribeTopics(topicNames, nil)
	if err != nil {
		slog.Error(constants.ERROR_CONSUMER_INITIALIZATION,
			slog.Any("error", err))
		return err
	}

	log.Println("Successfully subscribe to topics:")
	for _, name := range topicNames {
		log.Println(name)
	}

	// Process messages
	run := true
	eventCount := 0
	messages := make(map[string][]*kafka.Message) // Storage for messages to be batch processed. Our case requires a map for messages of keys.

	for run {
		select {
		case sig := <-ctx.Done():
			fmt.Printf("Caught signal %v: terminating consumer \n", sig)

			// process last batch here
			if eventCount > 0 {
				fmt.Println("Processing last batch")
				k.BatchHandle(&messages, &eventCount)
			}

			run = false
		default:
			ev, err := k.c.ReadMessage(100 * time.Millisecond)
			if err != nil {
				if key, ok := err.(kafka.Error); ok && key.Code() == kafka.ErrTimedOut {
					// In case of a timeout, do not wait reaching the BATCH_SIZE. Process stored messages.
					if len(messages) > 0 {
						k.BatchHandle(&messages, &eventCount)
					}
				}

				// Errors are informational and automatically handled by the consumer
				continue
			}

			// add message to batch storage
			messages[string(ev.Key)] = append(messages[string(ev.Key)], ev)
			eventCount++

			if eventCount%constants.BATCH_SIZE == 0 {
				k.BatchHandle(&messages, &eventCount)
			}
		}
	}

	return nil
}
