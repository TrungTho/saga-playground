package subscriber

import (
	"log/slog"

	"github.com/go-faker/faker/v4"
)

func (s Subscriber) PullSuccessfulCheckoutMessage() {
	runId := faker.UUIDDigit()
	logFields := slog.Group("worker",
		slog.String("name", "PullSuccessfulCheckoutMessage"),
		slog.String("runId", runId),
	)
	// ctx := context.Background()
	slog.Info("JOB START", logFields)

	// pull all records from kafka (do we need limit here?)

	// bulk insert to db

	// ack

	slog.Info("JOB END", logFields)
}

// todo: worker for processing record
// todo: worker for deleting already-processed records before 3 days ago
