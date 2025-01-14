package db

import (
	"context"
	"fmt"
	"log/slog"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// DBStore defines all functions to execute db queries and transactions
type DBStore interface {
	Querier
	CancelOrderTx(ctx context.Context, orderId int, logFields slog.Attr) (int, error)
	ValidateAndUpdateOrderStatusTx(ctx context.Context, id int, expectedCurrentStatus OrderStatus, newStatus OrderStatus, logFields slog.Attr) (orderId int, err error)
}

// SQLStore is the real implementation of querier that sqlc generated from migration script (in order to differentiate with mock one)
type SQLStore struct {
	*Queries
	db *pgxpool.Pool
}

func NewStore(db *pgxpool.Pool) DBStore {
	return &SQLStore{
		db:      db,
		Queries: New(db),
	}
}

func (store *SQLStore) execTx(ctx context.Context, fn func(*Queries) error) error {
	tx, err := store.db.BeginTx(ctx,
		pgx.TxOptions{
			IsoLevel: pgx.RepeatableRead,
		})
	if err != nil {
		return err
	}

	// New is provided by SQLC
	q := New(tx)

	// Catch the error of a transaction
	err = fn(q)
	if err != nil {
		if rbErr := tx.Rollback(ctx); rbErr != nil {
			return fmt.Errorf("tx err: %v, rb err: %v", err, rbErr)
		}
		return err
	}

	return tx.Commit(ctx)
}
