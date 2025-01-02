package util

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

type dummy struct {
	Name  string
	Age   int
	Email string
}

func TestConvertStructToByte(t *testing.T) {
	assert.NotPanics(t, func() {
		tmp := dummy{
			Name:  "this is a name",
			Age:   12,
			Email: "thisisanemail@mailmail.com",
		}

		res := ConvertStructToByte(t, tmp)

		require.NotNil(t, res)
	})
}

func TestConvertByteToStruct(t *testing.T) {
	assert.NotPanics(t, func() {
		tmp := dummy{
			Name:  "this is a name",
			Age:   12,
			Email: "thisisanemail@mailmail.com",
		}

		data := ConvertStructToByte(t, tmp)

		var res dummy
		ConvertByteToStruct(t, data, &res)

		require.Equal(t, tmp.Name, res.Name, "name should be equal")
		require.Equal(t, tmp.Age, res.Age, "age should be equal")
		require.Equal(t, tmp.Email, res.Email, "email should be equal")
	})
}
