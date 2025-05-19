package kafkaclient

import (
	"bytes"
	"log"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestNewConsumer(t *testing.T) {
	require.NotNil(t, testKafkaOperation, "Consumer should be set up with test container")

	// set up log assertion
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	// passing global ctx in order to stop listener before closing the consumer
	go testKafkaOperation.SubscribeTopics(globalContext, []string{"test-topic"})

	time.Sleep(100 * time.Millisecond) // to be sure that topic is subscribe

	require.True(t, strings.Contains(buf.String(), "Successfully subscribe to topics"))
}
