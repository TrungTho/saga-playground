package logger

import (
	"log/slog"
	"os"
)

func InitLogger() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level:     slog.LevelDebug,
		AddSource: true, // location of log line
	}))

	slog.SetDefault(logger)
}
