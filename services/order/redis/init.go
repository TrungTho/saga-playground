package redis

import (
	"context"
	"fmt"
	"log"
	"log/slog"
	"time"

	"github.com/TrungTho/saga-playground/util"
	"github.com/bsm/redislock"
	"github.com/redis/go-redis/v9"
)

type RedisStore struct {
	redisClient *redis.Client
	locker      *redislock.Client
}

func NewRedisWrapper(config util.Config) *RedisStore {
	rdb := redis.NewClient(&redis.Options{
		Addr:            fmt.Sprintf("%s:%s", config.REDIS_HOST, config.REDIS_PORT),
		Password:        config.REDIS_PASSWORD,
		DB:              config.REDIS_DB,
		PoolSize:        10,
		MinIdleConns:    2,
		ConnMaxIdleTime: 5 * time.Minute,
	})

	_, err := rdb.Ping(context.Background()).Result()
	if err != nil {
		log.Fatal("Failed to ping redis", err)
	}

	locker := redislock.New(rdb)
	if locker == nil {
		log.Fatal("Failed to init distributed lock", err)
	}

	return &RedisStore{
		redisClient: rdb,
		locker:      locker,
	}
}

func (r RedisStore) TryAcquireLock(ctx context.Context, key string, ttl time.Duration) *redislock.Lock {
	lock, err := r.locker.Obtain(ctx, key, ttl, nil)
	if err != nil {
		slog.ErrorContext(ctx, "Failed to acquire lock", slog.Any("error", err))
		return nil
	}

	return lock
}

func (r RedisStore) ReleaseLock(ctx context.Context, lock *redislock.Lock) {
	if lock != nil {
		lock.Release(ctx)
	}
}
