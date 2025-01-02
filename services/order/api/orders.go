package api

import (
	"math/big"
	"net/http"
	"strconv"

	db "github.com/TrungTho/saga-playground/db/sqlc"
	errorHandler "github.com/TrungTho/saga-playground/error"
	"github.com/TrungTho/saga-playground/util"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jinzhu/copier"
	log "github.com/sirupsen/logrus"
)

type CreateOrderRequest struct {
	UserId string `json:"user_id" binding:"required"`
}

// DTO for hiding some fields from DB (maybe in the future)
type CreateOrderResponse struct {
	ID int32 `json:"id"`
	// random value, not used now
	UserID string         `json:"user_id"`
	Status db.OrderStatus `json:"status"`
	Amount pgtype.Numeric `json:"amount"`
	// for failed reason
	Message *string `db:"message" json:"message"`
}

func (server *RestServer) createOrder(ctx *gin.Context) {
	var req CreateOrderRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, errorHandler.ErrorResponse(err))
		return
	}

	// auth is skipped in order to focus on business logic of this playground
	fakeAmount := float64(util.RandomInt(1, 100)) + util.RandomFloat(1)

	args := db.CreateOrderParams{
		UserID: req.UserId,
		Status: db.OrderStatusCreated,
	}

	args.Amount = pgtype.Numeric{
		Int:   big.NewInt(int64(fakeAmount * 100)),
		Exp:   -2,
		Valid: true,
	}

	createdOrder, err := server.dbStore.CreateOrder(ctx, args)
	errorHandler.HandleDbQueryError(err, ctx)

	var resp CreateOrderResponse
	copier.Copy(&resp, &createdOrder)

	ctx.JSON(http.StatusOK, resp)
}

func (server *RestServer) getOrder(ctx *gin.Context) {
	id := ctx.Param("id")
	orderId, err := strconv.Atoi(id)
	if err != nil {
		log.Errorf("Can not parse order id, got %v", id)
		errorHandler.HandleGeneralError(err, ctx, http.StatusBadRequest)
		return
	}

	order, err := server.dbStore.GetOrder(ctx, int32(orderId))
	errorHandler.HandleDbQueryError(err, ctx)

	var resp CreateOrderResponse
	copier.Copy(&resp, &order)

	ctx.JSON(http.StatusOK, resp)
}
