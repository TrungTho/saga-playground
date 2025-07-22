# build phase
# FROM docker.elastic.co/beats-dev/golang-crossbuild:1.23.4-arm AS builder
FROM --platform=linux/arm64 golang:1.23-alpine3.22 AS builder

# Install UPX, which will help to compress the final executable
RUN apk add --no-cache \
    upx \
    build-base

RUN ls -la /usr/bin

# ARG CGO_ENABLED=0 # can't enable because confluence-go is based on the C library librdkafka
ARG GOOS=linux
ARG GOARCH=arm64
ARG CC=/usr/bin/aarch64-alpine-linux-musl-gcc
WORKDIR /app

# download go dependencies
COPY go.mod go.sum ./
RUN go mod download

COPY . .

# for detail of complex tags, please check here: https://github.com/confluentinc/confluent-kafka-go/blob/master/README.md#librdkafka
# build executable
RUN CGO_ENABLED=1 GOOS=${GOOS} GOARCH=${GOARCH} CC=${CC} \
    go build \
    -ldflags '-extldflags "-static"' \
    -tags musl \
    -o order-service \
    # -v \
    ./cmd/server/main.go

#compress executable
RUN upx --best --lzma order-service

# run phase
FROM scratch
COPY --from=builder /app/order-service /order-service
ENTRYPOINT ["/order-service"]
