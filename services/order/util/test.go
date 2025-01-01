package util

import (
	"bytes"
	"encoding/json"
	"io"
	"testing"

	"github.com/stretchr/testify/require"
)

func ConvertStructToByte(t *testing.T, body interface{}) *bytes.Buffer {
	res, err := json.Marshal(body)
	require.NoError(t, err, "Should be able to convert body")
	return bytes.NewBuffer(res)
}

func ConvertByteToStruct(t *testing.T, body *bytes.Buffer, gotValue interface{}) {
	data, err := io.ReadAll(body)
	require.NoError(t, err, "Cannot parse body data")

	err = json.Unmarshal(data, &gotValue)
	require.NoError(t, err, "Cannot parse json string to struct")
}
