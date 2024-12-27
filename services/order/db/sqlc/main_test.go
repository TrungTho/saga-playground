package db

import (
	"context"
	"fmt"
	"log"
	"os"
	"testing"

	"github.com/TrungTho/saga-playground/util"
	"github.com/jackc/pgx/v5"
)

var testQueries *Queries

// This is kind of integration test with actual DB connection
// TODO: prepare mock DB for this in CI to avoid touching real dev DB
// NOTE: don't forget to run migration on that test DB
func TestMain(m *testing.M) {
	config, err := util.LoadConfig("../../../../.env")
	if err != nil {
		log.Fatal("cannot load config:", err)
	}

	// hardcode localhost db to prevent directly connect to actual DB
	dbSource := fmt.Sprintf("postgres://%s:%s@localhost:5432/%s?sslmode=disable",
		config.DB_USER, config.DB_PASSWORD, config.ORDER_DB_NAME)
	conn, err := pgx.Connect(context.Background(), dbSource)
	if err != nil {
		log.Fatal("cannot connect to db:", err)
	}

	testQueries = New(conn)
	os.Exit(m.Run()) // m.Run() will return the final code of tests -> exit the program with the same code
}
