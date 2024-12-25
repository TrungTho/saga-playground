include .env
DB_URL=postgres://${DB_USER}:${DB_PASSWORD}@localhost:5432/${DB_NAME}?sslmode=disable

migrateup:
	migrate -path db/migrations -database "$(DB_URL)" -verbose up

migrateup1:
	migrate -path db/migrations -database "$(DB_URL)" -verbose up 1

migratedown:
	migrate -path db/migrations -database "$(DB_URL)" -verbose down

migratedown1:
	migrate -path db/migrations -database "$(DB_URL)" -verbose down 1

new_migration:
	migrate create -ext sql -dir db/migration -seq $(name)
	
sqlc:
	sqlc generate

test:
	go test -v -cover -short ./...

run:
	go run main.go

.PHONY: migrateup migratedown migrateup1 migratedown1 new_migration sqlc test run