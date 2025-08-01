package db

import (
	"context"
	"fmt"
	"log"

	"github.com/TrungTho/saga-playground/util"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/jackc/pgx/v5/pgxpool"
)

func SetupDBConnection(config *util.Config) (DBStore, *pgxpool.Pool) {
	// set up db connection
	dbSource := fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable",
		config.DB_USER, config.DB_PASSWORD, config.DB_HOST, config.DB_PORT, config.ORDER_DB_NAME)
	testDB, err := pgxpool.New(context.Background(), dbSource)
	if err != nil {
		log.Fatal("Can not connect to the db: ", err)
	}

	// try to start migration up process
	m, err := migrate.New(config.ORDER_MIGRATION_FILE, dbSource)
	if err != nil {
		log.Fatal("Can not set up connection for db migration", err)
	}

	if err = m.Up(); err != nil && err != migrate.ErrNoChange {
		log.Fatal("Can not migrate db successfully", err)
	}

	// return connected & migrated db connection
	return NewStore(testDB), testDB
}
