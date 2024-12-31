package api

import (
	"net/http"

	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/util"
	"github.com/gin-gonic/gin"
)

type RestServer struct {
	// config    util.Config
	router    *gin.Engine
	dbQueries db.Querier
}

// NewServer creates a new HTTP server and set up routing.
func NewServer(config util.Config, dbQueries db.Querier) (*RestServer, error) {
	server := &RestServer{
		// config:    config,
		dbQueries: dbQueries,
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

	server.router = router
}

// Start runs the HTTP server on a specific address.
func (server *RestServer) Start(address string) error {
	return server.router.Run(address)
}
