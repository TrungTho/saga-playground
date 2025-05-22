package kafkaclient_test

import (
	"context"
	"fmt"
	"log"
	"os"
	"testing"

	kafkaclient "github.com/TrungTho/saga-playground/kafka"
	"github.com/TrungTho/saga-playground/util"
	"github.com/docker/go-connections/nat"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/kafka"
)

var (
	packageConfig      util.Config
	testKafkaOperation kafkaclient.KafkaOperations
	globalContext      context.Context
)

// This is kind of integration test with actual DB connection
func TestMain(m *testing.M) {
	config, err := util.LoadConfig("./kafka.example.env")
	if err != nil {
		log.Fatal("cannot load config:", err)
	}

	// set up test container
	ctx, cancel := context.WithCancel(context.Background())
	globalContext = ctx

	fmt.Println("Start test container")

	kafkaContainer, err := kafka.Run(globalContext,
		"confluentinc/confluent-local:7.5.0",
		kafka.WithClusterID("test-cluster"),
	)
	if err != nil {
		log.Printf("failed to start container: %s", err)
		cancel()
		return
	}

	mappedPort, err := kafkaContainer.MappedPort(globalContext, nat.Port("9093/tcp"))
	fmt.Println("Kafka mapped port:", mappedPort)
	if err != nil {
		log.Fatalln("Error when getting mapped port ", err)
	}

	// override variable for testing purpose
	config.KAFKA_BOOTSTRAP_HOST = "localhost"       // override to prevent test interact with real db instance
	config.KAFKA_BOOTSTRAP_PORT = mappedPort.Port() // the random exported port in host machine

	packageConfig = config

	testKafkaOperation, err = kafkaclient.NewKafkaStore(config, nil)
	if err != nil {
		log.Fatalln("Can't init kafka store for testing")
	}

	testCode := m.Run() // m.Run() will return the final code of tests -> exit the program with the same code

	// clean up resource

	// done context
	cancel()

	// shutdown consumer connection
	testKafkaOperation.Close()

	// terminate test container

	if err := testcontainers.TerminateContainer(kafkaContainer); err != nil {
		fmt.Printf("failed to terminate container: %v", err.Error())
	}

	fmt.Println("Test container is terminated!")

	os.Exit(testCode)
}
