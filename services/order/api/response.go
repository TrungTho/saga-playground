package api

import (
	"net/http"

	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
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
	resp := newResponse(http.StatusOK, "OK", data)

	c.JSON(http.StatusOK, resp)
}

func responseNoContent(c *gin.Context) {
	resp := newResponse(http.StatusNoContent, "OK", nil)

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
	log.WithFields(log.Fields{
		"request": c.Request,
	}).Errorf(err)

	resp := newResponse(http.StatusInternalServerError, err, nil)

	c.JSON(http.StatusInternalServerError, resp)
}

func responseBadRequest(c *gin.Context, err string) {
	resp := newResponse(http.StatusBadRequest, "Invalid request", err)

	c.JSON(http.StatusBadRequest, resp)
}

func responseNotFound(c *gin.Context, err string) {
	resp := newResponse(http.StatusNotFound, "Not found", err)

	c.JSON(http.StatusNotFound, resp)
}
