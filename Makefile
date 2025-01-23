include .env
GOPATH:=$(shell go env GOPATH)

####################
#     LOCAL DEV    #
####################

.PHONY: init
init: order.init

.PHONY: up
up: start
	podman compose -f deploys/docker-compose.yaml up -d

.PHONY: down
down: 
	podman compose -f deploys/docker-compose.yaml down

.PHONY: start
start: 
	podman ps || podman machine start

.PHONY: stop
stop: down
	podman machine stop
	@if ! podman ps &> /dev/null ; then \
		echo "successfully stop all containers!!!"; \
	else \
		echo "something wrong, podman is still running..."; \
	fi;

.PHONY: git_status
git_status:
	git status;
	@echo "Are you sure? [y/N] " && read ans && [ $${ans:-N} = y ]

.PHONY: amend_commit
amend_commit: git_status
	git add . && git commit --amend --no-edit
####################
#       KAFKA      #
####################
.PHONY: kafka.access
kafka.access:
	podman exec -it saga-kafka sh

.PHONY: kafka.log
kafka.log:
	podman logs saga-kafka

.PHONY: kafka.topic.create
kafka.topic.create:
	podman exec -it saga-kafka kafka-topics --bootstrap-server localhost:29092 --create --topic first_topic --replication-factor 2 --partitions 5	
	podman exec -it saga-kafka kafka-topics --bootstrap-server localhost:29092 --create --topic db.saga_playground.order.created --replication-factor 2 --partitions 5	

####################
#       DB         #
####################

.PHONY: db.access
db.access:
	podman access -it saga-database psql --username=${DB_USER} --dbname=${ORDER_DB_NAME}

####################
#       ORDER      #
####################
ORDER_DB_URL=postgres://${DB_USER}:${DB_PASSWORD}@localhost:5432/${ORDER_DB_NAME}?sslmode=disable

.PHONY: order.init
order.init:
	ln .githooks/* .git/hooks/ || echo "Hooks are already linked";

	@if ! which protoc &> /dev/null ; then \
		brew install protobuf; \
	else \
		echo "Protobuf already existed!"; \
	fi;

	@if ! which migrate &> /dev/null ; then \
		brew install golang-migrate; \
	else \
		echo "Go migrate already existed!"; \
	fi;

	@if ! which sqlc &> /dev/null ; then \
		brew install sqlc; \
		cd services/order && go install github.com/sqlc-dev/sqlc/cmd/sqlc@latest; \
	else \
		echo "sqlc already existed!"; \
	fi;

	@if ! which mockgen &> /dev/null ; then \
		cd services/order && go install go.uber.org/mock/mockgen@latest; \
	else \
		echo "mockgen already existed!"; \
	fi;

	@if ! which evans &> /dev/null ; then \
		brew tap ktr0731/evans; \
		brew install evans; \
	else \
		echo "mockgen already existed!"; \
	fi;

	@if test ! -f ${GOPATH}/bin/protoc-gen-go; then \
		cd services/order && go install google.golang.org/protobuf/cmd/protoc-gen-go@latest \
		cd services/order && go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest; \
	fi;

.PHONY: order.migrate.up
order.migrate.up:
	migrate -path services/order/db/migrations/ -database "$(ORDER_DB_URL)" -verbose up $(LEVEL)

.PHONY: order.migrate.down
order.migrate.down:
	migrate -path services/order/db/migrations/ -database "$(ORDER_DB_URL)" -verbose down $(LEVEL)

.PHONY: order.migrate.new
order.migrate.new:
	migrate create -ext sql -dir services/order/db/migrations/ -seq $(NAME)
	
.PHONY: order.sqlc
order.sqlc:
	sqlc generate -f services/order/sqlc.yaml

.PHONY: order.mock.generate
order.mock.generate:
	cd services/order/ && mockgen -destination ./db/mock/querier.go ./db/sqlc/ Querier
	cd services/order/ && mockgen -destination ./db/mock/dbstore.go ./db/sqlc/ DBStore

.PHONY: order.test order.test.unit order.test.integration
order.test: up order.vet order.test.unit order.test.integration
	@echo "Finished testing"

order.test.unit:
	cd services/order && go clean -cache && go test -v -race -cover -short ./...

order.test.integration:
	@echo "integration test to be implemented"

.PHONY:order.run
order.run: up order.vet 
	cd services/order && go run main.go

.PHONY: order.vet
order.vet:
	cd services/order && go vet ./...

.PHONY: order.tidy
order.tidy:
	cd services/order && go mod tidy -v
	cd services/order && go fmt ./...

.PHONY: order.protoc
order.protoc:
	rm -rf services/order/pb/*.go 
	protoc --proto_path=services/order/proto --go_out=services/order/pb --go_opt=paths=source_relative \
	--go-grpc_out=services/order/pb --go-grpc_opt=paths=source_relative \
	services/order/proto/*.proto

.PHONY: order.evans
order.evans:
	cd services/order && evans repl --proto ./proto/*.proto --host localhost --port 8081;