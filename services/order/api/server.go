package api

import (
	"net/http"

	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/gin-gonic/gin"
)

type RestServer struct {
	// config    util.Config // just in case we need to use it in the future
	router  *gin.Engine
	dbStore db.DBStore
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
		ctx.JSON(http.StatusOK, "pong")
	})

	router.POST("/orders", server.createOrder)
	router.GET("/orders/:id", server.getOrder)
	router.DELETE("/orders/:id", server.cancelOrder)

	server.router = router
}

// Start runs the HTTP server on a specific address.
func (server *RestServer) Start(address string) error {
	return server.router.Run(address)
}
