package error

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

func HandleGeneralError(err error, ctx *gin.Context, statusCode int) {
	switch statusCode {
	case http.StatusInternalServerError:
		// more business process can be applied here
		ctx.JSON(http.StatusInternalServerError, ErrorResponse(err))

	default:
		ctx.JSON(statusCode, ErrorResponse(err))
	}
}
