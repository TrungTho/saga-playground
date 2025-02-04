package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/TrungTho/saga-playground/worker/subscriber"
	"github.com/go-co-op/gocron/v2"
)

func main() {
	// create a scheduler
	s, err := gocron.NewScheduler()
	if err != nil {
		log.Fatalf("Failed to init gocron scheduler %v", err)
	}

	// for signal termination
	signChan := make(chan os.Signal, 1)

	// add a job to the scheduler
	registerFinishedCheckoutMessagePulling(s)

	// start the scheduler
	go s.Start()

	// block until you are ready to shut down
	signal.Notify(signChan, os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT, syscall.SIGTERM, syscall.SIGSEGV, syscall.SIGINT)
	<-signChan

	// graceful shutdown and clean resources if needed
	log.Println("Start shutting down server")
	err = s.Shutdown()
	if err != nil {
		log.Fatalf("Failed to stop scheduler %v", err)
	}

	close(signChan)
}

func registerFinishedCheckoutMessagePulling(s gocron.Scheduler) {
	j, err := s.NewJob(
		gocron.DurationJob(
			1*time.Second,
		),
		gocron.NewTask(
			subscriber.PullSuccessfulCheckoutMessage,
		),
		gocron.WithSingletonMode(gocron.LimitModeReschedule),
	)
	if err != nil {
		log.Fatalf("Failed to add job %v to scheduler %v", j.ID(), err)
	}

	// each job has a unique id
	fmt.Println("ID of job:", j.ID())
}
