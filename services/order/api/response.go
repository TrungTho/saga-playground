package api

import (
	"log/slog"
	"net/http"

	"github.com/TrungTho/saga-playground/constants"
	"github.com/gin-gonic/gin"
)

// RestResponse api response struct
type RestResponse struct {
	Code int         `json:"code"` // just in case in the future we need more than just a http code to evaluate the response (from client side)
	Msg  string      `json:"msg"`
	Data interface{} `json:"data"`
}

func newResponse(code int, msg string, data interface{}) *RestResponse {
	return &RestResponse{
		Code: code,
		Msg:  msg,
		Data: data,
	}
}

func responseSuccess(c *gin.Context, data interface{}) {
	resp := newResponse(http.StatusOK, constants.OK, data)

	c.JSON(http.StatusOK, resp)
}

func responseNoContent(c *gin.Context) {
	resp := newResponse(http.StatusNoContent, constants.OK, nil)

	c.JSON(http.StatusNoContent, resp)
}

func responseError(c *gin.Context, errCode int, err error, extras ...string) {
	resp := newResponse(
		errCode,
		err.Error(),
		nil,
	)

	c.JSON(errCode, resp)
}

func responseInternalServer(c *gin.Context, err string) {
	slog.ErrorContext(c, constants.ERROR_INTERNAL, slog.Group("request",
		slog.String("url", c.Request.RequestURI),
		slog.String("method", c.Request.Method),
		slog.Any("body", c.Request.Body),
		slog.String("error", err),
	),
	)

	resp := newResponse(http.StatusInternalServerError, err, nil)

	c.JSON(http.StatusInternalServerError, resp)
}

func responseBadRequest(c *gin.Context, err string) {
	resp := newResponse(http.StatusBadRequest, constants.ERROR_BAD_REQUEST, err)

	c.JSON(http.StatusBadRequest, resp)
}

func responseNotFound(c *gin.Context, err string) {
	resp := newResponse(http.StatusNotFound, constants.ERROR_NOT_FOUND, err)

	c.JSON(http.StatusNotFound, resp)
}
