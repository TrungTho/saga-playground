package main

import (
	"fmt"
	"log"
	"log/slog"
	"os"

	"github.com/TrungTho/saga-playground/api"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/util"
	"github.com/jackc/pgx/v5/pgxpool"
)

func main() {
	// load config from env file
	config := loadConfig()

	// init logger
	initLogger()

	// init db connection
	dbStores, db := initDbConnection(&config)
	defer db.Close()

	// init rest server configuration
	restServer := initRestServer(dbStores)

	// start rest server
	startRestServer(restServer, config)
}

func initLogger() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level:     slog.LevelDebug,
		AddSource: true, // location of log line
	}))

	slog.SetDefault(logger)
}

func startRestServer(restServer *api.RestServer, config util.Config) {
	err := restServer.Start(fmt.Sprintf("%s:%s",
		config.ORDER_SERVICE_HOST, config.ORDER_SERVICE_PORT))
	if err != nil {
		log.Fatalln("Can not start Rest Server ", err)
	}
}

func initRestServer(dbStore db.DBStore) *api.RestServer {
	restServer, err := api.NewServer(dbStore)
	if err != nil {
		log.Fatalln("Can not create new Rest Server ", err)
	}
	return restServer
}

func initDbConnection(config *util.Config) (db.DBStore, *pgxpool.Pool) {
	dbStore, db := db.SetupDBConnection(config)

	if dbStore == nil {
		log.Fatalln("Can not establish DB connection")
	}
	return dbStore, db
}

func loadConfig() util.Config {
	config, err := util.LoadConfig("./../../.env")
	if err != nil {
		log.Fatalln("Can not load config from env ", err)
	}
	return config
}
