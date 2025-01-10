package db

import (
	"fmt"
	"log"
	"os"
	"testing"

	"github.com/TrungTho/saga-playground/util"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/require"
)

var (
	testQueries *Queries
	testStore   DBStore
)

// This is kind of integration test with actual DB connection
func TestMain(m *testing.M) {
	config, err := util.LoadConfig("../../../../.env")
	if err != nil {
		log.Fatal("cannot load config:", err)
	}

	// run migration on db before testing
	config.ORDER_MIGRATION_FILE = "file://../../db/migrations/"
	config.DB_HOST = "localhost" // override to prevent test interact with real db instance
	var testDB *pgxpool.Pool
	testStore, testDB = SetupDBConnection(&config)
	testQueries = New(testDB)
	defer testDB.Close()

	os.Exit(m.Run()) // m.Run() will return the final code of tests -> exit the program with the same code
}

func TestModel(t *testing.T) {
	expectedNumberOfStatuses := 8
	require.Equal(t, expectedNumberOfStatuses, len(AllOrderStatusValues()), fmt.Sprintf("There should be %v statuses, modify this test when you add more statuses", expectedNumberOfStatuses))

	val := OrderStatus("hihi")
	require.Equal(t, false, val.Valid(), "Invalid status should fails validation")

	val = OrderStatusCreated
	require.Equal(t, true, val.Valid(), "Valid status should passes validation")
}
