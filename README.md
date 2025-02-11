### Components

- order (Golang) service
  - publish message to queue new pending_order `(1)` (with random amount values, no items)
  - API Endpoint for polling order statuses
    - created
    - pendingPayment (checkout service pick up order and start checkout process)
    - awaitingPayment (checkout successfully but no webhook confirm from payment gateway)
    - awaitingFulfillment (finish payment phase) - pivot in saga
    - failed
    - refunded
    - canceled
    - finished
  - API Endpoint for cancelling an order
  - gRPC endpoint for successfully checkout order
- checkout (Springboot) service, subscribe to `(1)` and handle following flow:
  - Check account balance (no deduction, only comparision) → fail order if account balance isn’t sufficient to pay (publish message to failed_order queue `(2)`)
    - otherwise publish message to queue paid_order `(3)`
- fulfillment (NodeJS) service,
  - subscribe to queue `(3)` to process all post-payment service:
  - subscribe to queue `(2)` in order to send noti to user
  - some time-consuming functions & publish message to success_order `(4)`
  - outbox transaction pattern for message publishing for worker thread testing

### Tasks

- [ ] Order service
  - [ ] DB
    - [x] Docker compose for DB
    - [x] DB & Tables init
    - [x] DB migration plan
    - [x] DB interaction by using sqlc
    - [x] Embedded migration to server [ref](https://github.com/golang-migrate/migrate?tab=readme-ov-file#use-in-your-go-project)
  - [ ] Server
    - [ ] APIs
      - [x] API for handling new order creation
      - [x] API for handling get order detail
      - [x] API for handling cancel an order (only applicable if order.status=created)
    - [ ] Workers
      - [ ] Transactional inbox pattern for status update (simple 1 thread for pulling message & N worker threads for processing)
        - [ ] Message pulling worker (with redis lock for race condition)
          - [ ] Crobjob configuration (1 min interval because status does not need to be updated so frequently)
          - [ ] Redis integration and testing
          - [ ] Bulk insert into `order_status_changes` table and testing
        - [ ] Message processing worker
          - [ ] Count number of un-processed changes
          - [ ] Batching processing changes (re-consider the strategy here, which primary should be for table schema, include multi-servers case)
        - [ ] Worker for processed records deletion (3 days retention)
        - [ ] Thread pool configuration for all worker
    - [x] General response format
      - [x] Constant string based error code instead of string
    - [x] gRPC endpoint to start checkout on order (switch status to pendingPayment)
    - [x] Consumer for status changes from other's service topics
      - Consider to implement transaction inbox pattern if choosing to use Debezium for message publishing (checkout service)
    - [x] Consider message publishing pattern
      - [x] ~~Out-box transaction pattern (Obsoleted -> choose Debezium)~~
      - [x] Debezium
        - [x] Configure Debezium with current components
        - [x] Bootstrapping script for connector registration
        - [x] Topic prefix rename
    - [x] Logging set up and refactor for all error cases
  - [ ] Testing
    - [ ] Unit tests
      - [ ] DB repository function tests
        - [x] orders.sql.go
        - [x] ordertx.go
        - [x] util package
      - [ ] Server tests
        - [x] POST /orders
        - [x] GET /orders/:id
        - [x] DELETE /orders/:id (cancel an order)
        - [x] gRPC functions unit test
    - [ ] Integration test
      - [x] Fake data for test DB strategies
- [ ] Kafka cluster
  - [x] Configure & spin up cluster using docker-compose (revise concept with courses)
  - [x] Configure & smoke test topic & partition
  - [x] Data bootstrap strategy (topic & partition configuration) -> Using customized entrypoint with Kafka CLI
  - [ ] Check dead letter configuration & modify consumers
- [ ] Checkout service
  - [ ] Server
    - [x] Init Springboot server with dependencies (update init in Makefile)
    - [x] Health-check API & general response
      - [x] Handle 404 path instead of throwing exception
    - [ ] Workers
      - [x] Pull data from Kafka, save to DB (checkout_inbox table) & and send ack to Kafka
      - [ ] Process data (main logic, but just fake it)
        - [ ] gRPC call to order service
      - [ ] Manually poll for payment status
      - [ ] Outbox pattern for post-payment phases
    - [ ] APIs
      - [ ] API for manually resolve checkout (just auto resolve it)
      - [ ] API for handling confirmed payment webhook (payment captured)
    - [ ] Transactional inbox pattern for order checkout processing (pull from kafka -> store to inbox table -> send ack to kafka -> trigger event to listener to process)
      - [ ] Be careful with already-processed orders (check internal DB before starting the logic)
      - [ ] Background worker for failed message listener trigger (crash before triggering or crash when processing) -> batch process
        - [ ] disable consumer offset auto commit -> use transaction to save messages to db + commit offset to satisfy at least one delivery
      - [ ] Consider removing/moving processed records -> check for best practices here
        - [ ] Option1: using redis for processed record's offset -> increasing only
  - [ ] DB
    - [x] Liquidbase integration
    - [x] Checkout inbox & checkout table schema definition
  - [ ] Testing <<---- focus here
    - [ ] Kafka testing
    - [ ] DB testing
    - [ ] Unit test APIs and workers
- [ ] Fulfillment service
- [ ] Tech debt
  - [ ] Using init container for scripting bootstrap instead of overwriting image entrypoint
  - [ ] Move order tables to order_shema schema instead of public for consistency
  - [ ] Error monitoring integration (sentry?)
  - [ ] Checkout service logging configuration
  - [ ] Consider for DLQ demo instead of just omit the error
  - [ ] Main function recover for order service/worker
  - [ ] Graceful shutdown for services
    - [x] Order service
    - [ ] Checkout service
- [ ] Repository
  - [x] Hook for commit message validation
  - [ ] CI for quality control
    - [ ] Bot to comment test results (coverage) to PR
    - [ ] Container service for integration tests
  - [ ] CD for images packaging to ghcr.io
- [ ] Documenting (considering to use `make init` instead of plain documentation)
  - [ ] Hooks usage when cloning repository (`ln .githooks/* .git/hooks/`)
  - [ ] DB schema generation by using dbdiagram export
  - [ ] Migration generation with `migrate create -ext sql -dir services/order/db/migrations -seq <file_name>`
  - [ ] Golang query generation by using sqlc
  - [ ] Mock test with go mock
  - [ ] gRPC code generation with protobuf
  - [ ] Overall architecture, each service's responsibility (simplified version explanation) and points to enhancement
    - [ ] User actions in checkout phase (unhappy case)
    - [ ] Notification service
- [ ] Misc
  - [x] Makefile
  - [ ] Consider Order Notification service for status changed notification (and also required user interaction actions)
    - Technical to apply: transaction inbox pattern, batching processing with goroutines, event grammar

### DB design

- Order table:
  - id: string
  - status: enum
  - amount: decimal
- Checkout table:
  - id: string
  - order_id: string
  - status: enum (failed, pending, successful)
- Fulfillment table:
  - id: string
  - order_id: string
  - status: enum (failed, pending, successful)

### Tooling decision

- BE services: Golang + SpringBoot + NodeJS
- DB: sqlite
- Messaging: Kafka
- Load test: k6
- CI: Github Actions
- CD: ArgoCD + k8s
- Monitoring: Prometheus + Grafana
- Outbox transaction pattern: Bare implementation in Order service (Golang) and debezium for Checkout service (Spring)
- DB change migration: golang migrate for Golang (manual migration), embedded Liquibase for Spring (auto migration)

### Topics

- order_pending (simplifier version of pending_transaction), all orders that are waiting for checkout at payment service
  - order_id
  - total
- order_failed for all services to subscribe & process the rollback (order_id)
- checkout_failed (order_id)
- checkout_successful (order_id)
- fulfill_failed (order_id)
- fulfill_successful (order_id)

### CI

- Unit tests
- Images building & publishing to ghcr

### CD:

- ArgoCD set up for infra
- Optional Keda for queue

### Non functional requirements

- Unit tests with coverage (CI constraint with > 90% coverage criteria)
- Integration test with test container
- API spec (swagger)
- Database migration

### Flows

- Happy case:

  - full flow from place order to successfully notification received

  ```mermaid
  sequenceDiagram
    actor c as Client
    participant o as order_service
    participant ck as checkout_service
    participant f as fulfillment_service

    c->>o: Place new order (HTTP)
    o-->>ck: Request checkout (Kafka)
    ck->>o: Check valid order and update status (gRPC)
    alt invalid order (user already cancelled order)
      c->>o: Cancel order
      o->>o: Check & update status
      o-->>ck: stop process
    else valid order
      o->>o: Check & update status
      o-->>ck: valid order status
      c->>o: Cancel order
      o-->>c: Invalid action (need manual process)
      ck->>ck: Check account balance (random)
      alt failed
        ck-->>o: Update status to failed
      else successfully
        ck->>o: Update status to processing
        ck-->>f: Request post-payment process
        f->>f: Processing
        alt failed
          f-->>ck: Request post-payment process
          f-->>o: Update status to failed
        else successfully
        end
        f-->>o: Update status to finished
      end
    end
  ```
