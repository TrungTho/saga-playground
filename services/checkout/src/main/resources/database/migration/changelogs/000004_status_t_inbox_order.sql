CREATE TYPE "inbox_order_status" AS ENUM (
  'NEW',
  'IN_PROGRESS',
  'FAILED',
  'DONE'
);

ALTER TABLE "t_inbox_order"
ADD COLUMN "status" inbox_order_status NOT NULL;