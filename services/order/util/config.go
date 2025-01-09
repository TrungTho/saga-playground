package util

import (
	"fmt"

	"github.com/spf13/viper"
)

// Config stores all configuration of the application.
// The values are read by viper from a config file or environment variable.
type Config struct {
	DB_PASSWORD string `mapstructure:"DB_PASSWORD"`
	DB_HOST     string `mapstructure:"DB_HOST"`
	DB_USER     string `mapstructure:"DB_USER"`
	DB_DRIVER   string `mapstructure:"DB_DRIVER"`

	ORDER_DB_NAME           string `mapstructure:"ORDER_DB_NAME"`
	ORDER_SERVICE_PORT      string `mapstructure:"ORDER_SERVICE_PORT"`
	ORDER_SERVICE_GRPC_PORT string `mapstructure:"ORDER_SERVICE_GRPC_PORT"`
	ORDER_SERVICE_HOST      string `mapstructure:"ORDER_SERVICE_HOST"`
}

// LoadConfig reads configuration from file or environment variables.
func LoadConfig(path string) (config Config, err error) {
	viper.SetConfigFile(path)

	viper.AutomaticEnv()

	err = viper.ReadInConfig()
	if err != nil {
		panic(fmt.Errorf("fatal error config file: %w", err))
	}

	err = viper.Unmarshal(&config)
	return
}
