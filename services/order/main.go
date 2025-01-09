package main

import (
	"fmt"
	"log"
	"log/slog"
	"net"
	"os"

	"github.com/TrungTho/saga-playground/api"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/grpc_server"
	"github.com/TrungTho/saga-playground/pb"
	"github.com/TrungTho/saga-playground/util"
	"github.com/jackc/pgx/v5/pgxpool"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
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
	go startRestServer(restServer, config)

	// init rest gRPC server configuration
	gRPCServer := initGRPCServer(dbStores)

	// start grpc server
	startGRPCServer(gRPCServer, config)
}

func startGRPCServer(logicServer *grpc_server.GRPCServer, config util.Config) {
	grpcServer := grpc.NewServer()
	pb.RegisterOrderServiceServer(grpcServer, logicServer) // register the "logic server", which implement all the needed functions of generated interfaces - logicServer, with the shell, actual, skeleton gRPC server to serve incoming traffic - grpcServer (grpc.NewServer)

	reflection.Register(grpcServer) // auto explore all defined grpc functions we implement inside of gRPCServer

	grpcAddress := fmt.Sprintf("%s:%s",
		config.ORDER_SERVICE_HOST, config.ORDER_SERVICE_GRPC_PORT)

	log.Printf("Start gRPC server at %v\n", grpcAddress)
	lis, err := net.Listen("tcp", grpcAddress)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	err = grpcServer.Serve(lis)
	if err != nil {
		log.Fatalf("cannot start grpc server")
	}
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

func initGRPCServer(dbStore db.DBStore) *grpc_server.GRPCServer {
	gRPCServer, err := grpc_server.NewServer(dbStore)
	if err != nil {
		log.Fatalln("Can not create new gRPC Server ", err)
	}
	return gRPCServer
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
