package kafkaclient

import (
	"bytes"
	"context"
	"log"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestNewConsumer(t *testing.T) {
	consumer, err := NewConsumer(packageConfig)

	require.Nil(t, err, "Error should be nil")
	require.NotNil(t, consumer, "Consumer should be set up with test container")

	kafkaStore := &KafkaStore{
		c: consumer,
		p: nil,
	}

	ctx := context.Background()

	// set up log assertion
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	go kafkaStore.SubscribeTopics(ctx, []string{"test-topic"})

	time.Sleep(100 * time.Millisecond) // to be sure that topic is subscribe

	require.True(t, strings.Contains(buf.String(), "Successfully subscribe to topics"))
}
