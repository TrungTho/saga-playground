package db

import (
	"context"
	"fmt"

	"github.com/TrungTho/saga-playground/util"
	"github.com/jackc/pgx/v5"
	log "github.com/sirupsen/logrus"
)

func SetupDBConnection(config *util.Config) *Queries {
	dbSource := fmt.Sprintf("postgres://%s:%s@%s:5432/%s?sslmode=disable",
		config.DB_USER, config.DB_PASSWORD, config.DB_HOST, config.ORDER_DB_NAME)
	conn, err := pgx.Connect(context.Background(), dbSource)
	if err != nil {
		log.Fatal("cannot connect to db:", err)
	}

	return New(conn)
}
