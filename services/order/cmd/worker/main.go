package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/TrungTho/saga-playground/logger"
	"github.com/go-co-op/gocron/v2"
)

func main() {
	// init logger
	logger.InitLogger()

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
	go func() {
		s.Start()
		log.Println("Successfully start worker!!!")
	}()

	// block until you are ready to shut down
	signal.Notify(signChan, os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT, syscall.SIGTERM, syscall.SIGSEGV, syscall.SIGINT)
	<-signChan

	// graceful shutdown and clean resources if needed
	log.Println("Start shutting down workers...")
	log.Printf("Number of active jobs: %v - number of in-queue jobs %v\n", len(s.Jobs()), s.JobsWaitingInQueue())
	err = s.Shutdown()
	if err != nil {
		log.Fatalf("Failed to stop scheduler %v", err)
	}

	log.Println("Finished shutting down workers!!!")
	close(signChan)
}

func registerFinishedCheckoutMessagePulling(s gocron.Scheduler) {
	j, err := s.NewJob(
		gocron.DurationJob(
			1*time.Second,
		),
		gocron.NewTask(
			func() {
				log.Println("start job")
				time.Sleep(10 * time.Second)
				log.Println("finished job")
			},
		),
		gocron.WithSingletonMode(gocron.LimitModeReschedule),
	)
	if err != nil {
		log.Fatalf("Failed to add job %v to scheduler %v", j.ID(), err)
	}

	// each job has a unique id
	fmt.Println("ID of job:", j.ID())
}
