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
  "userId" varchar NOT NULL,
  "status" order_status NOT NULL,
  "amount" decimal(5,2) NOT NULL,
  "message" varchar DEFAULT '',
  "created_at" timestamp,
  "updated_at" timestamp
);

COMMENT ON COLUMN "order"."userId" IS 'random value, not used now';

COMMENT ON COLUMN "order"."message" IS 'for failed reason';
