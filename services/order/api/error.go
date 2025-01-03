package api

import (
	"database/sql"
	"net/http"

	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
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

func handleDbQueryError(err error, ctx *gin.Context) {
	if err != nil {
		// errCode := db.ErrorCode(err)
		// if errCode == db.ForeignKeyViolation || errCode == db.UniqueViolation {
		// 	ctx.JSON(http.StatusForbidden, errorResponse(err))
		// 	return
		// }
		// ctx.JSON(http.StatusInternalServerError, errorResponse(err))
		// return
		log.Error("Error in DB ", err)

		if err == sql.ErrNoRows {
			handleGeneralError(err, ctx, http.StatusNotFound)
			return
		}
		handleGeneralError(err, ctx, http.StatusInternalServerError)
		return
	}
}
