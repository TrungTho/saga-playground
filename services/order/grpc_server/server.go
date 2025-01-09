package grpc_server

import (
	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/pb"
)

type GRPCServer struct {
	// config    util.Config // just in case we need to use it in the future
	dbStore db.DBStore
	pb.UnimplementedOrderServiceServer
}

// NewServer creates a new HTTP server and set up routing.
func NewServer(dbStore db.DBStore) (*GRPCServer, error) {
	server := &GRPCServer{
		dbStore: dbStore,
	}

	return server, nil
}

// Start runs the HTTP server on a specific address.
func (server *GRPCServer) Start(address string) error {
	return nil
}
