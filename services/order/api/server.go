package api

import (
	"context"
	"log"
	"net/http"

	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/gin-gonic/gin"
)

type RestServer struct {
	router  *gin.Engine
	dbStore db.DBStore
	server  *http.Server
}

// NewServer creates a new HTTP server and set up routing.
func NewServer(dbStore db.DBStore) (*RestServer, error) {
	server := &RestServer{
		dbStore: dbStore,
	}

	server.setupRouter()
	return server, nil
}

func (server *RestServer) setupRouter() {
	router := gin.Default()

	// health check endpoint
	router.GET("/ping", func(ctx *gin.Context) {
		responseSuccess(ctx, "pong")
	})

	router.POST("/orders", server.createOrder)
	router.GET("/orders/:id", server.getOrder)
	router.DELETE("/orders/:id", server.cancelOrder)

	server.router = router
}

// Start runs the HTTP server on a specific address.
func (s *RestServer) Start(address string) error {
	log.Default().Printf("Start REST server at %v", address)

	s.server = &http.Server{
		Addr:    address,
		Handler: s.router.Handler(),
	}

	return s.server.ListenAndServe()
}

// Start runs the HTTP server on a specific address.
func (s *RestServer) Stop() error {
	return s.server.Shutdown(context.Background()) // unlimited time
}
