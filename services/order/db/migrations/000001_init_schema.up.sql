CREATE TYPE "order_status" AS ENUM (
  'created',
  'processing',
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
  "created_at" timestamp,
  "updated_at" timestamp
);

COMMENT ON COLUMN "orders"."user_id" IS 'random value, not used now';

COMMENT ON COLUMN "orders"."message" IS 'for failed reason';
