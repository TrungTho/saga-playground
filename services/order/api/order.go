package api

import (
	"math/big"
	"net/http"

	db "github.com/TrungTho/saga-playground/db/sqlc"
	errorHandler "github.com/TrungTho/saga-playground/error"
	"github.com/TrungTho/saga-playground/util"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jinzhu/copier"
)

type createOrderRequest struct {
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
	var req createOrderRequest
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

	createdOrder, err := server.dbQueries.CreateOrder(ctx, args)
	errorHandler.HandleDbQueryError(err, ctx)

	var resp CreateOrderResponse
	copier.Copy(&resp, &createdOrder)

	ctx.JSON(http.StatusOK, resp)
}
