package kafkaclient

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/TrungTho/saga-playground/constants"
	"github.com/TrungTho/saga-playground/util"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func NewConsumer(config util.Config) *kafka.Consumer {
	c, err := kafka.NewConsumer(&kafka.ConfigMap{
		// User-specific properties that you must set
		"bootstrap.servers": fmt.Sprintf("%s:%s", config.KAFKA_BOOTSTRAP_HOST, config.KAFKA_BOOTSTRAP_PORT),

		// Fixed properties
		"group.id":           constants.CONSUMER_GROUP_ID,
		"auto.offset.reset":  constants.AUTO_OFFSET_RESET_MODE,
		"enable.auto.commit": false, // we want to handle event in batches
	})
	if err != nil {
		log.Fatalln("Can't establish Kafka connection")
	}

	return c
}

func (k *KafkaStore) SubscribeTopics(ctx context.Context, topicNames []string) {
	err := k.c.SubscribeTopics(topicNames, nil)
	if err != nil {
		log.Fatalln("Can't subscribe for topic", err)
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

			// todo: process last batch here
			if eventCount > 0 {
				fmt.Println("Processing last batch")
				invokeBatchProcessing(&messages, &eventCount)
			}

			run = false
		default:
			ev, err := k.c.ReadMessage(100 * time.Millisecond)
			if err != nil {
				if k, ok := err.(kafka.Error); ok && k.Code() == kafka.ErrTimedOut {
					// In case of a timeout, do not wait reaching the BATCH_SIZE. Process stored messages.
					if len(messages) > 0 {
						invokeBatchProcessing(&messages, &eventCount)
					}
				}

				// Errors are informational and automatically handled by the consumer
				continue
			}

			// add message to batch storage
			messages[string(ev.Key)] = append(messages[string(ev.Key)], ev)
			eventCount++

			if eventCount%constants.BATCH_SIZE == 0 {
				invokeBatchProcessing(&messages, &eventCount)
			}
		}
	}
}

func invokeBatchProcessing(messages *map[string][]*kafka.Message, eventCount *int) {
	// invoke real logic to process messages
	doSomething(messages, eventCount)

	// reset counter and batch storage
	*messages = make(map[string][]*kafka.Message)
	*eventCount = 0
}

func doSomething(messages *map[string][]*kafka.Message, eventCount *int) {
	// todo: handle failed processing case
	fmt.Println("Processing", *eventCount, "records")
	for key, values := range *messages {
		fmt.Println("This is topic: ", *(values[0].TopicPartition.Topic))
		fmt.Println("This is key ", key, "and values: ")
		for _, val := range values {
			fmt.Printf("%v\t", string(val.Value))
		}
		fmt.Printf("\n======\n")
	}
}
