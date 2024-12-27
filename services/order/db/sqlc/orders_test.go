package db

import (
	"context"
	"fmt"
	"math/big"
	"testing"

	"github.com/TrungTho/saga-playground/util"
	"github.com/go-faker/faker/v4"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/stretchr/testify/require"
)

func createNewTestOrder(msg string, status OrderStatus) (Order, error) {
	args := CreateOrderParams{}
	err := faker.FakeData(&args)
	if err != nil {
		fmt.Println(err)
	}

	*args.Message = msg
	args.Status = status

	fakeAmount := float64(util.RandomInt(1, 100)) + util.RandomFloat(1)

	args.Amount = pgtype.Numeric{
		Int:   big.NewInt(int64(fakeAmount * 100)),
		Exp:   -2,
		Valid: true,
	}

	return testQueries.CreateOrder(context.Background(), args)
}

func TestCreateOrder(t *testing.T) {
	createdOrder, err := createNewTestOrder("", OrderStatusCreated)

	require.NoError(t, err, "Error should be nil")
	require.NotNil(t, createdOrder)
	require.NotEmpty(t, createdOrder.ID, "ID should be generated")
	require.NotEmpty(t, createdOrder.CreatedAt, "created_at should be generated")
	require.NotEmpty(t, createdOrder.UpdatedAt, "updated_at should be generated")
	require.Equal(t, OrderStatusCreated, createdOrder.Status, "status should be created")
	require.Empty(t, createdOrder.Message, "message should be empty by default")

	createdOrder, err = createNewTestOrder(faker.Sentence(), OrderStatusCreated)

	require.NoError(t, err, "Error should be nil")
	require.NotNil(t, createdOrder)
	require.NotEmpty(t, createdOrder.Message, "message should not empty if provided")
}

func TestGetOrder(t *testing.T) {
	createdOrder, err := createNewTestOrder("", OrderStatusCreated)

	require.NoError(t, err, "Error should be nil")
	require.NotNil(t, createdOrder)

	res, err := testQueries.GetOrder(context.Background(), createdOrder.ID)
	require.NoError(t, err, "Error should be nil")
	require.NotNil(t, res)
	require.Equal(t, createdOrder.Amount, res.Amount, "amount should be equal")
	require.Equal(t, createdOrder.ID, res.ID, "id should be equal")
	require.Equal(t, createdOrder.UserID, res.UserID, "userid should be equal")
	require.Equal(t, createdOrder.Status, res.Status, "status should be equal")
}

func TestListOrders(t *testing.T) {
	quantity := 3
	for i := 0; i < quantity; i++ {
		createNewTestOrder(faker.Sentence(), OrderStatusCreated)
	}

	res, err := testQueries.ListOrders(context.Background(),
		ListOrdersParams{Limit: int32(quantity), Offset: 0})

	require.NoError(t, err, "Error should be nil")
	require.NotNil(t, res)
	require.Equal(t, quantity, len(res), "size should be equal")
	require.NotNil(t, res[0], "element should not be nil")
}

func TestUpdateOrder(t *testing.T) {
	createdOrder, err := createNewTestOrder(faker.Sentence(), OrderStatusCreated)

	require.NoError(t, err, "Error should be nil")
	require.NotNil(t, createdOrder)

	res, err := testQueries.UpdateOrderStatus(context.Background(),
		UpdateOrderStatusParams{
			ID:     createdOrder.ID,
			Status: OrderStatusFailed,
		})
	require.NoError(t, err, "Error should be nil")
	require.NotNil(t, res)
	require.Equal(t, createdOrder.Amount, res.Amount, "amount should be equal")
	require.Equal(t, createdOrder.ID, res.ID, "id should be equal")
	require.Equal(t, createdOrder.UserID, res.UserID, "userid should be equal")
	require.NotEqual(t, createdOrder.Status, res.Status, "status should not be equal")
	require.Equal(t, OrderStatusFailed, res.Status, "status should be failed")
}
