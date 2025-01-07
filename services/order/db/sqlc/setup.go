package db

import (
	"context"
	"fmt"
	"log"

	"github.com/TrungTho/saga-playground/util"
	"github.com/jackc/pgx/v5/pgxpool"
)

func SetupDBConnection(config *util.Config) (DBStore, *pgxpool.Pool) {
	dbSource := fmt.Sprintf("postgres://%s:%s@%s:5432/%s?sslmode=disable",
		config.DB_USER, config.DB_PASSWORD, config.DB_HOST, config.ORDER_DB_NAME)
	testDB, err := pgxpool.New(context.Background(), dbSource)
	if err != nil {
		log.Fatal("Can not connect to the db: ", err)
	}

	return NewStore(testDB), testDB
}
