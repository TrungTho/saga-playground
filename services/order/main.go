package main

import (
	"fmt"
	"log"

	"github.com/TrungTho/saga-playground/api"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/util"
)

func main() {
	// load config from env file
	config := loadConfig()

	// init db connection
	dbQueries := initDbConnection(&config)

	// init rest server configuration
	restServer := initRestServer(config, dbQueries)

	// start rest server
	startRestServer(restServer, config)
}

func startRestServer(restServer *api.RestServer, config util.Config) {
	err := restServer.Start(fmt.Sprintf("%s:%s",
		config.ORDER_SERVICE_HOST, config.ORDER_SERVICE_PORT))
	if err != nil {
		log.Fatalln("Can not start Rest Server ", err)
	}
}

func initRestServer(config util.Config, dbQueries *db.Queries) *api.RestServer {
	restServer, err := api.NewServer(config, dbQueries)
	if err != nil {
		log.Fatalln("Can not create new Rest Server ", err)
	}
	return restServer
}

func initDbConnection(config *util.Config) *db.Queries {
	dbQueries := db.SetupDBConnection(config)

	if dbQueries == nil {
		log.Fatalln("Can not establish DB connection")
	}
	return dbQueries
}

func loadConfig() util.Config {
	config, err := util.LoadConfig("./../../.env")
	if err != nil {
		log.Fatalln("Can not load config from env ", err)
	}
	return config
}
