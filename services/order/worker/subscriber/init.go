package subscriber

import (
	"github.com/TrungTho/saga-playground/redis"
)

type Subscriber struct {
	redisStore *redis.RedisStore
}
