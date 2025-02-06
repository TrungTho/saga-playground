package util

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestLoadConfig(t *testing.T) {
	// expectedValue := "DB_DRIVER_VALUE"
	config, err := LoadConfig("example.env")

	require.NoError(t, err, "error should be nil")
	require.NotNil(t, config, "config should not be nil")
	// require.Equal(t, expectedValue, config.DB_DRIVER, "value should be loaded successfully")
}

func TestLoadConfigFailed(t *testing.T) {
	require.Panics(t, func() { LoadConfig("dummyName") }, "error should be thrown")
}
