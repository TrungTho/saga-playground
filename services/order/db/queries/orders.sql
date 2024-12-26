-- name: CreateOrder :one
INSERT INTO orders (
  user_id,
  status,
  amount,
  message
) VALUES (
  $1, $2, $3, $4
) RETURNING *;

-- name: GetOrder :one
SELECT * FROM orders
WHERE id = $1 LIMIT 1;

-- name: ListOrders :many
SELECT * FROM orders
ORDER BY id
LIMIT $1
OFFSET $2;

-- name: UpdateOrderStatus :one
UPDATE orders
  set status = $2
WHERE id = $1
RETURNING *;