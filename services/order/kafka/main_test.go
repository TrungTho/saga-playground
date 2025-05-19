package kafkaclient

import (
	"context"
	"fmt"
	"log"
	"os"
	"testing"

	"github.com/TrungTho/saga-playground/util"
	"github.com/docker/go-connections/nat"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/kafka"
)

var packageConfig util.Config

// This is kind of integration test with actual DB connection
func TestMain(m *testing.M) {
	config, err := util.LoadConfig("./kafka.example.env")
	if err != nil {
		log.Fatal("cannot load config:", err)
	}

	// set up test container
	ctx := context.Background()

	fmt.Println("Start test container")

	kafkaContainer, err := kafka.Run(ctx,
		"confluentinc/confluent-local:7.5.0",
		kafka.WithClusterID("test-cluster"),
	)
	if err != nil {
		log.Printf("failed to start container: %s", err)
		return
	}

	mappedPort, err := kafkaContainer.MappedPort(ctx, nat.Port("9093/tcp"))
	fmt.Println("Kafka mapped port:", mappedPort)
	if err != nil {
		log.Fatalln("Error when getting mapped port ", err)
	}

	// override variable for testing purpose
	config.KAFKA_BOOTSTRAP_HOST = "localhost"       // override to prevent test interact with real db instance
	config.KAFKA_BOOTSTRAP_PORT = mappedPort.Port() // the random exported port in host machine

	packageConfig = config

	testCode := m.Run() // m.Run() will return the final code of tests -> exit the program with the same code

	if err := testcontainers.TerminateContainer(kafkaContainer); err != nil {
		fmt.Printf("failed to terminate container: %v", err.Error())
	}

	fmt.Println("Test container is terminated!")

	os.Exit(testCode)
}
