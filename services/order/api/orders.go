package api

import (
	"fmt"
	"math/big"
	"strconv"

	db "github.com/TrungTho/saga-playground/db/sqlc"
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
		responseBadRequest(ctx, err.Error())
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
	handleDbQueryError(err, ctx)

	var resp CreateOrderResponse
	copier.Copy(&resp, &createdOrder)

	responseSuccess(ctx, resp)
}

func (server *RestServer) getOrder(ctx *gin.Context) {
	id := ctx.Param("id")
	orderId, err := strconv.Atoi(id)
	if err != nil {
		errStr := fmt.Sprintf("Can not parse order id, got %v", id)
		log.Error(errStr)
		responseBadRequest(ctx, errStr)
		return
	}

	order, err := server.dbStore.GetOrder(ctx, int32(orderId))
	handleDbQueryError(err, ctx)

	var resp CreateOrderResponse
	copier.Copy(&resp, &order)

	responseSuccess(ctx, resp)
}

func (server *RestServer) cancelOrder(ctx *gin.Context) {
	id := ctx.Param("id")
	orderId, err := strconv.Atoi(id)
	if err != nil {
		errStr := fmt.Sprintf("Can not parse order id, got %v", id)
		log.Error(errStr)
		responseBadRequest(ctx, errStr)
		return
	}

	_, err = server.dbStore.CancelOrderTx(ctx, orderId)
	if err != nil {
		responseBadRequest(ctx, err.Error())
		return
	}

	responseNoContent(ctx)
}
