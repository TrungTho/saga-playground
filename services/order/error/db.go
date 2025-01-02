package error

import (
	"database/sql"
	"net/http"

	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
)

func HandleDbQueryError(err error, ctx *gin.Context) {
	if err != nil {
		// errCode := db.ErrorCode(err)
		// if errCode == db.ForeignKeyViolation || errCode == db.UniqueViolation {
		// 	ctx.JSON(http.StatusForbidden, errorResponse(err))
		// 	return
		// }
		// ctx.JSON(http.StatusInternalServerError, errorResponse(err))
		// return
		log.Error("Error in DB", err)

		if err == sql.ErrNoRows {
			HandleGeneralError(err, ctx, http.StatusNotFound)
			return
		}
		HandleGeneralError(err, ctx, http.StatusInternalServerError)
		return
	}
}

func ErrorResponse(err error) gin.H {
	return gin.H{"error": err.Error()}
}
