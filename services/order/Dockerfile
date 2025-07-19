# build phase
FROM golang:1.23-alpine3.22 AS builder

# Install UPX, which will help to compress the final executable
RUN apk add --no-cache \
    upx \
    gcc \
    clang \
    g++ \
    mold \
    libc-dev \
    librdkafka-dev \
    pkgconf \
    musl-dev \
    cyrus-sasl-dev

# ARG CGO_ENABLED=0 # can't enable because confluence-go is based on the C library librdkafka
ARG GOOS=darwin
ARG GOARCH=arm64
WORKDIR /app

# download go dependencies
COPY go.mod go.sum ./
RUN go mod download

COPY . .

# for detail of complex tags, please check here: https://github.com/confluentinc/confluent-kafka-go/blob/master/README.md#librdkafka
# build executable
RUN CGO_ENABLED=1 GOOS=${GOOS} GOARCH=${GOARCH} \
    CGO_LDFLAGS="-fuse-ld=mold -lsasl2" \
    go build -tags musl -ldflags '-extldflags "-static"' \
    -o order-service \
    ./cmd/server/main.go

#compress executable
# RUN upx --best --lzma myapp

# run phase
FROM scratch
COPY --from=builder /app/order-service /order-service
ENTRYPOINT ["/order-service"]
