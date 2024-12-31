include .env

####################
#     LOCAL DEV    #
####################

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
stop: 
	podman machine stop

.PHONY: git_status
git_status:
	git status;
	@echo "Are you sure? [y/N] " && read ans && [ $${ans:-N} = y ]

.PHONY: amend_commit
amend_commit: git_status
	git add . && git commit --amend --no-edit

####################
#       ORDER      #
####################
ORDER_DB_URL=postgres://${DB_USER}:${DB_PASSWORD}@localhost:5432/${ORDER_DB_NAME}?sslmode=disable

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

.PHONY: order.test order.test.unit order.test.integration
order.test: order.test.unit order.test.integration

order.test.unit:
	cd services/order && go clean -cache && go test -v -race -cover -short ./...

order.test.integration:
	echo "integration test to be implemented"

.PHONY: order.run
order.run:
	cd services/order && go run main.go

.PHONY: order.tidy
order.tidy:
	cd services/order && go mod tidy -v
	cd services/order && go fmt ./...
