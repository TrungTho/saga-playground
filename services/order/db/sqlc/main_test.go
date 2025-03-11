package db

import (
	"context"
	"fmt"
	"log"
	"os"
	"testing"
	"time"

	"github.com/TrungTho/saga-playground/util"
	"github.com/docker/go-connections/nat"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/network"
	"github.com/testcontainers/testcontainers-go/wait"
)

var (
	testQueries *Queries
	testStore   DBStore
)

// This is kind of integration test with actual DB connection
func TestMain(m *testing.M) {
	config, err := util.LoadConfig("../../util/example.env")
	if err != nil {
		log.Fatal("cannot load config:", err)
	}

	// set up test container
	ctx := context.Background()

	fmt.Println("Start test container")
	testNetwork := []string{"test-network"}
	pgContainer, err := postgres.Run(ctx,
		"debezium/postgres:14-alpine",
		postgres.WithDatabase(config.ORDER_DB_NAME),
		postgres.WithUsername(config.DB_USER),
		postgres.WithPassword(config.DB_PASSWORD),
		network.WithNewNetwork(ctx, testNetwork),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").
				WithOccurrence(2).WithStartupTimeout(5*time.Second),
			wait.ForListeningPort("5432/tcp"),
		),
	)
	if err != nil {
		log.Fatal("Can not init test container", err)
	}

	mappedPort, err := pgContainer.MappedPort(ctx, nat.Port("5432/tcp"))
	fmt.Println("Mapped port:", mappedPort)
	if err != nil {
		fmt.Println("Error when getting mapped port ", err)
	}

	// override variable for testing purpose
	config.ORDER_MIGRATION_FILE = "file://../../db/migrations/"
	config.DB_HOST = "localhost"       // override to prevent test interact with real db instance
	config.DB_PORT = mappedPort.Port() // the random exported port in host machine

	// connect to test container
	var testDB *pgxpool.Pool
	testStore, testDB = SetupDBConnection(&config)
	testQueries = New(testDB)
	defer testDB.Close()

	testCode := m.Run() // m.Run() will return the final code of tests -> exit the program with the same code

	// clean up before report final code
	if err := pgContainer.Terminate(ctx); err != nil {
		log.Fatal("Can not terminate test container")
	}

	log.Default().Println("Test container is terminated!")

	os.Exit(testCode)
}

func TestModel(t *testing.T) {
	expectedNumberOfStatuses := 8
	require.Equal(t, expectedNumberOfStatuses, len(AllOrderStatusValues()), fmt.Sprintf("There should be %v statuses, modify this test when you add more statuses", expectedNumberOfStatuses))

	val := OrderStatus("hihi")
	require.Equal(t, false, val.Valid(), "Invalid status should fails validation")

	val = OrderStatusCreated
	require.Equal(t, true, val.Valid(), "Valid status should passes validation")
}
