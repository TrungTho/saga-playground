# build phase
# FROM docker.elastic.co/beats-dev/golang-crossbuild:1.23.4-arm AS builder
FROM golang:1.23-alpine3.22 AS builder

# Install UPX, which will help to compress the final executable
RUN apk add --no-cache \
    upx \
    zig 

# build-base

# ARG CGO_ENABLED=0 # can't disable CGO because confluence-go is based on the C library librdkafka
ARG GOOS=linux
ARG GOARCH=arm64
ARG CC="zig cc -target aarch64-linux"
WORKDIR /app

# download go dependencies
COPY go.mod go.sum ./
RUN go mod download

COPY . .

# for detail of complex tags, please check here: https://github.com/confluentinc/confluent-kafka-go/blob/master/README.md#librdkafka
# build executable
RUN CGO_ENABLED=1 GOOS=${GOOS} GOARCH=${GOARCH} CC="${CC}" \
    go build \
    -ldflags '-extldflags "-static"' \
    -tags musl \
    -o order-service \
    -v \
    ./cmd/server/main.go

#compress executable
RUN upx --best --lzma order-service

# run phase
FROM scratch
COPY --from=builder /app/order-service /order-service
COPY --from=builder /app/db/migrations /db/migrations
ENTRYPOINT ["/order-service"]
