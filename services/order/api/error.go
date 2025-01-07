package api

import (
	"database/sql"
	"log/slog"
	"net/http"

	"github.com/gin-gonic/gin"
)

func handleGeneralError(err error, ctx *gin.Context, statusCode int) {
	switch statusCode {
	case http.StatusInternalServerError:
		// more business process can be applied here
		responseInternalServer(ctx, err.Error())

	default:
		responseError(ctx, statusCode, err)
	}
}

func handleDbQueryError(err error, logFields slog.Attr, ctx *gin.Context) {
	if err != nil {
		// errCode := db.ErrorCode(err)
		// if errCode == db.ForeignKeyViolation || errCode == db.UniqueViolation {
		// 	ctx.JSON(http.StatusForbidden, errorResponse(err))
		// 	return
		// }
		// ctx.JSON(http.StatusInternalServerError, errorResponse(err))
		// return
		// because the error didn't occur here, so source will be incorrect -> using invoker to add that information
		slog.Error("DB", slog.Any("error", err), logFields)

		if err == sql.ErrNoRows {
			handleGeneralError(err, ctx, http.StatusNotFound)
			return
		}
		handleGeneralError(err, ctx, http.StatusInternalServerError)
		return
	}
}
