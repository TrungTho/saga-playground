### Components

- order (Golang) service
  - publish message to queue new pending_order `(1)` (with random amount values, no items)
  - API Endpoint for polling order statuses
    - created
    - processing → finish payment & in fulfillment phase → pivot of saga, **`need to be update async`**
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
    - [ ] DB init
    - [ ] DB migration plan
  - [ ] API for handle new order creation
- [ ] Checkout service
- [ ] Fulfillment service
- [ ] Repository
  - [x] Hook for commit message validation
  - [ ] Document for hooks usage when cloning repository (`ln .githooks/* .git/hooks/`)

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
- CDC: TBD for transaction outbox pattern
- DB change migration: TBD Liquibase?

### Queues

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

    c->>o: Place new order
    o-->>ck: Request checkout
    ck->>ck: Check account balance
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
  ```

- Cancel order
  - If order status < processing → order service allow user to cancel it → what if order is in queue already? (checkout service will pick order)
    - share db?
