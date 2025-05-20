package kafkaclient_test

import (
	"bytes"
	"context"
	"errors"
	"log"
	"log/slog"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/TrungTho/saga-playground/constants"
	kafkaclient "github.com/TrungTho/saga-playground/kafka"
	mock_kafkaclient "github.com/TrungTho/saga-playground/kafka/mock"
	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/go-faker/faker/v4"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
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

func TestSubscribeTopics_CancelContext(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())

	mockTopicName := faker.Word() // random to make sure it unique

	// set up log assertion
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	mockKafkaStore, err := kafkaclient.NewKafkaStore(packageConfig)
	require.Nil(t, err, "Error should be nil for new kafka store creation")

	c := make(chan int)
	go func() {
		err = mockKafkaStore.SubscribeTopics(ctx, []string{mockTopicName})
		c <- 1 // inform main thread to resume
	}()

	go func() {
		time.Sleep(time.Second) // wait for 1 second
		cancel()
	}()

	<-c // stop to wait for the subscriber to return from ctx.Done()
	require.Nil(t, err, "Error should be nil")

	require.True(t, strings.Contains(buf.String(), "TERMINATING LISTENER"))
	mockKafkaStore.Close()
}

var testTopic = "test-topic"

func TestSubscribeTopics(t *testing.T) {
	testCases := []struct {
		testName      string
		mockTopicName []string
		buildStub     func(mockConsumer *mock_kafkaclient.MockKafkaConsumer) *kafkaclient.KafkaStore
		assertion     func(t *testing.T, err error, bufLog string)
	}{
		{
			testName:      "Failed to subscribe topic",
			mockTopicName: []string{"test-topic"},
			buildStub: func(mockConsumer *mock_kafkaclient.MockKafkaConsumer) *kafkaclient.KafkaStore {
				mockConsumer.EXPECT().SubscribeTopics(gomock.Any(), gomock.Any()).Return(errors.New("error"))
				return &kafkaclient.KafkaStore{
					Consumer: mockConsumer,
				}
			},
			assertion: func(t *testing.T, err error, bugLog string) {
				require.NotNil(t, err, "Error should NOT be nil")
				require.True(t, strings.Contains(bugLog, constants.ERROR_CONSUMER_INITIALIZATION))
			},
		},
		{
			testName:      "Process last batch before termination",
			mockTopicName: []string{"test-topic"},
			buildStub: func(mockConsumer *mock_kafkaclient.MockKafkaConsumer) *kafkaclient.KafkaStore {
				mockConsumer.EXPECT().SubscribeTopics(gomock.Any(), gomock.Any()).Return(nil)
				mockConsumer.EXPECT().ReadMessage(gomock.Any()).AnyTimes().Return(nil, errors.New("dummy error")) // in order for default case continue
				return &kafkaclient.KafkaStore{
					Consumer:     mockConsumer,
					MessageCount: 1,
				}
			},
			assertion: func(t *testing.T, err error, bugLog string) {
				require.Nil(t, err, "Error should be nil")
				require.True(t, strings.Contains(bugLog, "Processing last batch before terminating listeners"))
			},
		},
		{
			testName:      "Batching message in normal case",
			mockTopicName: []string{"test-topic"},
			buildStub: func(mockConsumer *mock_kafkaclient.MockKafkaConsumer) *kafkaclient.KafkaStore {
				mockConsumer.EXPECT().SubscribeTopics(gomock.Any(), gomock.Any()).Return(nil)
				mockConsumer.EXPECT().ReadMessage(gomock.Any()).AnyTimes().Return(&kafka.Message{
					Key: []byte(faker.Word()),
					TopicPartition: kafka.TopicPartition{
						Topic: &testTopic,
					},
				}, nil) // in order for default case continue
				mockConsumer.EXPECT().CommitMessage(gomock.Any()).AnyTimes().AnyTimes().DoAndReturn(func(_ any) ([]kafka.TopicPartition, error) {
					slog.Info("commit message")
					return nil, nil
				})

				return &kafkaclient.KafkaStore{
					Consumer:        mockConsumer,
					MessageCount:    0,
					MessageMap:      make(map[string][]*kafka.Message),
					MessageHandlers: make(map[string]kafkaclient.MessageHandler),
				}
			},
			assertion: func(t *testing.T, err error, bugLog string) {
				require.Nil(t, err, "Error should be nil")
				require.True(t, strings.Contains(bugLog, "batch count"))
				require.True(t, strings.Contains(bugLog, "commit message"))
			},
		},
		{
			testName:      "Timeout before batch size reached",
			mockTopicName: []string{"test-topic"},
			buildStub: func(mockConsumer *mock_kafkaclient.MockKafkaConsumer) *kafkaclient.KafkaStore {
				mockConsumer.EXPECT().SubscribeTopics(gomock.Any(), gomock.Any()).Return(nil)
				mockConsumer.EXPECT().ReadMessage(gomock.Any()).AnyTimes().Return(nil, kafka.NewError(kafka.ErrTimedOut, "DummyError", false))
				mockConsumer.EXPECT().CommitMessage(gomock.Any()).AnyTimes().AnyTimes().DoAndReturn(func(_ any) ([]kafka.TopicPartition, error) {
					slog.Info("commit message")
					return nil, nil
				})

				return &kafkaclient.KafkaStore{
					Consumer:        mockConsumer,
					MessageCount:    1,
					MessageMap:      make(map[string][]*kafka.Message),
					MessageHandlers: make(map[string]kafkaclient.MessageHandler),
				}
			},
			assertion: func(t *testing.T, err error, bugLog string) {
				require.Nil(t, err, "Error should be nil")
				require.True(t, strings.Contains(bugLog, "batch count"))
			},
		},
	}

	for _, tt := range testCases {
		t.Run(tt.testName, func(t *testing.T) {
			// set up log assertion
			var buf bytes.Buffer
			log.SetOutput(&buf)
			defer func() {
				log.SetOutput(os.Stderr)
			}()

			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			mockConsumer := mock_kafkaclient.NewMockKafkaConsumer(ctrl)
			mockKafkaStore := tt.buildStub(mockConsumer)

			mockCtx, cancel := context.WithCancel(context.Background())

			c := make(chan int)
			var err error
			go func() {
				err = mockKafkaStore.SubscribeTopics(mockCtx, tt.mockTopicName)
				c <- 1 // inform main thread to resume
			}()

			go func() {
				time.Sleep(2 * time.Second) // let the consumer run for 2 secs then simulate a shutdown
				t.Log("Sending cancel signal")
				cancel()
			}()

			<-c

			tt.assertion(t, err, buf.String())
		})
	}
}
