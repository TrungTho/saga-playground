CREATE TYPE "order_status" AS ENUM (
  'created',
  'pendingPayment',
  'awaitingPayment',
  'awaitingFulfillment',
  'failed',
  'refunded',
  'cancelled',
  'finished'
);

CREATE TABLE "orders" (
  "id" SERIAL PRIMARY KEY,
  "user_id" varchar NOT NULL,
  "status" order_status NOT NULL,
  "amount" decimal(5,2) NOT NULL,
  "message" varchar DEFAULT '',
  "created_at" timestamptz DEFAULT 'now()',
  "updated_at" timestamptz DEFAULT 'now()'
);

ALTER TABLE orders AUTO_INCREMENT = 1000;

COMMENT ON COLUMN "orders"."user_id" IS 'random value, not used now';

COMMENT ON COLUMN "orders"."message" IS 'for failed reason';
