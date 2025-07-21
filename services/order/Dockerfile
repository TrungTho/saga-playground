# build phase
# FROM docker.elastic.co/beats-dev/golang-crossbuild:1.23.4-arm AS builder
FROM golang:1.23-alpine3.22 AS builder

# Install UPX, which will help to compress the final executable
RUN apk add --no-cache \
    upx \
    build-base

RUN uname -a
# ARG CGO_ENABLED=0 # can't enable because confluence-go is based on the C library librdkafka
ARG GOOS=linux
ARG GOARCH=arm64
WORKDIR /app

# download go dependencies
COPY go.mod go.sum ./
RUN go mod download

COPY . .

# for detail of complex tags, please check here: https://github.com/confluentinc/confluent-kafka-go/blob/master/README.md#librdkafka
# build executable
RUN CGO_ENABLED=1 GOOS=${GOOS} GOARCH=${GOARCH} \
    go build \
    -ldflags '-extldflags "-static"' \
    -tags musl \
    -o order-service \
    ./cmd/server/main.go

#compress executable
# RUN upx --best --lzma myapp

# run phase
FROM scratch
COPY --from=builder /app/order-service /order-service
ENTRYPOINT ["/order-service"]
