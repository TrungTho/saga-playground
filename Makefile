include .env
export
GOPATH:=$(shell go env GOPATH)
JAVA_GRPC_PLUGIN:=$(shell which protoc-gen-grpc-java)
CHECKOUT_PROTO_DIR:="services/checkout/src/main/java/"
VM_NAME:=saga-vm
####################
#     LOCAL DEV    #
####################

.PHONY: init
init: order.init checkout.init

.PHONY: restart
restart: down up

.PHONY: up
up: start
	podman -c $(VM_NAME) compose -f deploys/docker-compose.yaml up -d

.PHONY: down
down: 
	podman -c $(VM_NAME) compose -f deploys/docker-compose.yaml down

.PHONY: start
start: 
	podman -c $(VM_NAME) ps || podman machine start ${VM_NAME}

.PHONY: stop
stop: down
	podman machine stop ${VM_NAME}
	@if ! podman -c $(VM_NAME) ps &> /dev/null ; then \
		echo "successfully stop all containers!!!"; \
	else \
		echo "something wrong, podman is still running..."; \
	fi;

.PHONY: test
test: order.test checkout.test

.PHONY: protoc
protoc: order.protoc checkout.protoc

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
	podman exec -it saga-kafka kafka-topics --bootstrap-server localhost:29092 --create --topic db.saga_playground.order.created --replication-factor 2 --partitions 5	

.PHONY: kafka.reset
kafka.reset:
	rm -rf deploys/kafka-volume
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
		echo "evans already existed!"; \
	fi;

	@if test ! -f ${GOPATH}/bin/protoc-gen-go; then \
		cd services/order && go install google.golang.org/protobuf/cmd/protoc-gen-go@latest \
		cd services/order && go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest; \
	fi;

	@if test ! -f ${GOPATH}/bin/go-test-coverage; then \
		cd services/order && go install github.com/vladopajic/go-test-coverage/v2@latest; \
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
	cd services/order/ && mockgen -destination ./kafka/mock/kafkastore.go ./kafka/ KafkaOperations
	cd services/order/ && mockgen -destination ./kafka/mock/consumer.go ./kafka/ KafkaConsumer

.PHONY: order.test order.test.unit order.test.integration
order.test: order.vet order.test.unit order.test.integration
	@echo "========================"
	@echo "====Finished testing===="
	@echo "========================"

.PHONY: order.test.unit
order.test.unit:
	cd services/order && go test -coverprofile=order-coverage.out -covermode=atomic -cover -short  ./...
	cd services/order && go-test-coverage --config ./.coverageconfig.yml
	# convert coverage data to html
	cd services/order && go tool cover -html=order-coverage.out -o order-coverage.html
	# remove .out file
	# rm services/order/order-coverage.out

.PHONY: order.test.coverage_render
order.test.coverage_render:
	cd services/order && go tool cover -html="coverage.out"

order.test.integration:
	@echo "integration test to be implemented"

.PHONY:order.run
order.run: 
	cd services/order && go run cmd/server/main.go

.PHONY:order.worker.run
order.worker.run:
	cd services/order && go run cmd/worker/main.go

.PHONY: order.vet
order.vet:
	cd services/order && go vet ./...

.PHONY: order.tidy
order.tidy:
	cd services/order && go mod tidy -v
	cd services/order && go fmt ./...

.PHONY: order.git.add
order.git.add:
	@git add services/order/
	
.PHONY: order.protoc
order.protoc:
	rm -rf services/order/pb/*.go 
	protoc --proto_path=deploys/proto --go_out=services/order/pb --go_opt=paths=source_relative \
	--go-grpc_out=services/order/pb --go-grpc_opt=paths=source_relative \
	deploys/proto/*.proto

.PHONY: order.evans
order.evans:
	evans repl --proto ./deploys/proto/*.proto --host localhost --port 8081;

####################
#     Checkout     #
####################
.PHONY: checkout.run
checkout.run:
	@if [ -z "${ck_port}" ]; then \
		cd services/checkout && ./gradlew bootRun; \
	else \
		cd services/checkout && CHECKOUT_SERVICE_PORT=$(ck_port) ./gradlew bootRun; \
	fi;

.PHONY: checkout.build
checkout.build:
	cd services/checkout && ./gradlew build

.PHONY: checkout.test
checkout.test:
	cd services/checkout && ./gradlew checkstyleTest && ./gradlew checkstyleMain && ./gradlew clean && ./gradlew check

.PHONY: checkout.git.add
checkout.git.add:
	@git add services/checkout/

.PHONY: checkout.init
checkout.init:
	@if ! which protoc-gen-grpc-java &> /dev/null ; then \
		brew install protoc-gen-grpc-java; \
	else \
		echo "protoc-gen-grpc-java already existed!"; \
	fi;

	@if ! which grpcurl &> /dev/null ; then \
		brew install grpcurl; \
	else \
		echo "grpcurl already existed!"; \
	fi;


.PHONY: checkout.protoc
checkout.protoc:
	# rm -rf $(CHECKOUT_PROTO_DIR)/*
	protoc --proto_path=deploys/proto \
	--plugin=protoc-gen-grpc-java=$(JAVA_GRPC_PLUGIN) \
 	--grpc-java_out=$(CHECKOUT_PROTO_DIR) \
 	--java_out=$(CHECKOUT_PROTO_DIR) \
	deploys/proto/*.proto
