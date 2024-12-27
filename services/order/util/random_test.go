package util

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestRandomInt(t *testing.T) {
	for i := 0; i < 100; i++ {
		lowerBound := int64(1)
		upperBound := int64(100)
		res := RandomInt(int64(lowerBound), int64(upperBound))

		require.NotEmpty(t, res, "value should not be empty")
		require.GreaterOrEqual(t, res, lowerBound)
		require.LessOrEqual(t, res, upperBound)
	}
}

func TestRandomFloat(t *testing.T) {
	base := 1
	for i := 0; i < 5; i++ {
		lowerBound := float64(0)
		upperBound := float64(base)
		res := RandomFloat(base)

		require.NotEmpty(t, res, "value should not be empty")
		require.GreaterOrEqual(t, res, lowerBound)
		require.LessOrEqual(t, res, upperBound)
		base *= 10
	}
}
