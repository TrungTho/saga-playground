package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"log/slog"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/TrungTho/saga-playground/api"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/grpc_server"
	"github.com/TrungTho/saga-playground/logger"
	"github.com/TrungTho/saga-playground/pb"
	"github.com/TrungTho/saga-playground/util"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/sync/errgroup"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {
	// load config from env file
	config := loadConfig()

	// init logger
	logger.InitLogger()

	// init db connection
	dbStores, db := initDbConnection(&config)
	defer db.Close()

	// block until you are ready to shut down
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT, syscall.SIGTERM, syscall.SIGSEGV, syscall.SIGINT)
	defer stop()

	wg, ctx := errgroup.WithContext(ctx)

	// init rest server configuration
	restServer := initRestServer(dbStores)

	// start rest server
	startRestServer(ctx, wg, restServer, config)

	// init rest gRPC server configuration
	gRPCServer := initGRPCServer(dbStores)

	// start grpc server
	startGRPCServer(ctx, wg, gRPCServer, config)

	err := wg.Wait()
	if err != nil {
		log.Fatal("failed to start servers", err)
	}

	// graceful shutdown and clean resources if needed
	log.Println("==============================")
	log.Println("Finished shutting down servers")
	log.Println("==============================")
}

func startGRPCServer(ctx context.Context,
	wg *errgroup.Group,
	logicServer *grpc_server.GRPCServer,
	config util.Config,
) {
	grpcServer := grpc.NewServer()
	pb.RegisterOrderServiceServer(grpcServer, logicServer) // register the "logic server", which implement all the needed functions of generated interfaces - logicServer, with the shell, actual, skeleton gRPC server to serve incoming traffic - grpcServer (grpc.NewServer)

	reflection.Register(grpcServer) // auto explore all defined grpc functions we implement inside of gRPCServer

	wg.Go(func() error {
		grpcAddress := fmt.Sprintf("%s:%s",
			config.ORDER_SERVICE_HOST, config.ORDER_SERVICE_GRPC_PORT)

		log.Printf("Start gRPC server at %v\n", grpcAddress)
		lis, err := net.Listen("tcp", grpcAddress)
		if err != nil && !errors.Is(err, grpc.ErrServerStopped) {
			slog.Error("grpc server failed to listen")
			return err
		}

		err = grpcServer.Serve(lis)
		if err != nil {
			slog.Error("grpc server failed to start")
			return err
		}

		return nil
	})

	wg.Go(func() error {
		<-ctx.Done()

		slog.InfoContext(ctx, "Shutting down grpc server...")
		grpcServer.GracefulStop()
		slog.InfoContext(ctx, "gRPC server is completely shutdown!")

		return nil
	})
}

func startRestServer(ctx context.Context,
	wg *errgroup.Group,
	restServer *api.RestServer,
	config util.Config,
) {
	wg.Go(func() error {
		err := restServer.Start(fmt.Sprintf("%s:%s",
			config.ORDER_SERVICE_HOST, config.ORDER_SERVICE_PORT))
		if err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Fatalln("Can not start Rest Server ", err)
			return err
		}

		return nil
	})

	wg.Go(func() error {
		<-ctx.Done()

		slog.InfoContext(ctx, "Shutting down rest server...")
		err := restServer.Stop()
		if err != nil {
			log.Fatalln("Can not stop Rest Server ", err)
			return err
		}

		slog.InfoContext(ctx, "RESTful server is completely shutdown!")
		return nil
	})
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
