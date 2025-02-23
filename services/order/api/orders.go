package api

import (
	"fmt"
	"log/slog"
	"math/big"
	"strconv"

	"github.com/TrungTho/saga-playground/constants"
	db "github.com/TrungTho/saga-playground/db/sqlc"
	"github.com/TrungTho/saga-playground/util"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jinzhu/copier"
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

	logFields := slog.Group("request",
		slog.String("URL", ctx.Request.URL.Path),
		slog.Any("Body", ctx.Request.Body),
	)

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
	if err != nil {
		slog.ErrorContext(ctx, constants.ERROR_ORDER_CREATE_FAILED, slog.Any("error", err))
		responseInternalServer(ctx, err.Error())
		return
	}

	var resp CreateOrderResponse
	err = copier.Copy(&resp, &createdOrder)
	if err != nil {
		slog.ErrorContext(ctx, constants.ERROR_ORDER_DTO_CONVERT, logFields)
		responseInternalServer(ctx, err.Error())
		return
	}

	slog.InfoContext(ctx, constants.ORDER_CREATED, logFields,
		slog.Any("order", createdOrder))

	responseSuccess(ctx, resp)
}

func (server *RestServer) getOrder(ctx *gin.Context) {
	logFields := slog.Group("request",
		slog.String("URL", ctx.Request.URL.Path),
	)

	id := ctx.Param("id")
	orderId, err := strconv.Atoi(id)
	if err != nil {
		errStr := fmt.Sprintf("Can not parse order id, got %v", id)
		slog.ErrorContext(ctx, errStr, logFields, "error", err)
		responseBadRequest(ctx, errStr)
		return
	}

	order, err := server.dbStore.GetOrder(ctx, int32(orderId))
	if err != nil {
		slog.ErrorContext(ctx, constants.ERROR_ORDER_NOT_FOUND, slog.Any("error", err))
		responseNotFound(ctx, err.Error())
		return
	}

	var resp CreateOrderResponse
	err = copier.Copy(&resp, &order)
	if err != nil {
		errStr := fmt.Sprintf("Can not clone order from %v", order)
		slog.ErrorContext(ctx, errStr, logFields)
		responseBadRequest(ctx, errStr)
		return
	}

	responseSuccess(ctx, resp)
}

func (server *RestServer) cancelOrder(ctx *gin.Context) {
	logFields := slog.Group("request",
		slog.String("URL", ctx.Request.URL.Path),
		slog.Any("Body", ctx.Request.Body),
	)

	id := ctx.Param("id")
	orderId, err := strconv.Atoi(id)
	if err != nil {
		errStr := fmt.Sprintf("Can not parse order id, got %v", id)
		slog.ErrorContext(ctx, errStr, logFields, "error", err)
		responseBadRequest(ctx, errStr)
		return
	}

	_, err = server.dbStore.CancelOrderTx(ctx, orderId, logFields)
	if err != nil {
		slog.ErrorContext(ctx, constants.ERROR_ORDER_CANCEL_FAILED, slog.Any("error", err))
		responseBadRequest(ctx, err.Error())
		return
	}

	slog.InfoContext(ctx, constants.ORDER_CANCELLED, logFields)

	responseNoContent(ctx)
}
