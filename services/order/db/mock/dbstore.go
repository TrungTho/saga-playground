// Code generated by MockGen. DO NOT EDIT.
// Source: ./db/sqlc/ (interfaces: DBStore)
//
// Generated by this command:
//
//	mockgen -destination ./db/mock/dbstore.go ./db/sqlc/ DBStore
//

// Package mock_db is a generated GoMock package.
package mock_db

import (
	context "context"
	slog "log/slog"
	reflect "reflect"

	db "github.com/TrungTho/saga-playground/db/sqlc"
	gomock "go.uber.org/mock/gomock"
)

// MockDBStore is a mock of DBStore interface.
type MockDBStore struct {
	ctrl     *gomock.Controller
	recorder *MockDBStoreMockRecorder
	isgomock struct{}
}

// MockDBStoreMockRecorder is the mock recorder for MockDBStore.
type MockDBStoreMockRecorder struct {
	mock *MockDBStore
}

// NewMockDBStore creates a new mock instance.
func NewMockDBStore(ctrl *gomock.Controller) *MockDBStore {
	mock := &MockDBStore{ctrl: ctrl}
	mock.recorder = &MockDBStoreMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockDBStore) EXPECT() *MockDBStoreMockRecorder {
	return m.recorder
}

// CancelOrderTx mocks base method.
func (m *MockDBStore) CancelOrderTx(ctx context.Context, orderId int, logFields slog.Attr) (int, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "CancelOrderTx", ctx, orderId, logFields)
	ret0, _ := ret[0].(int)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// CancelOrderTx indicates an expected call of CancelOrderTx.
func (mr *MockDBStoreMockRecorder) CancelOrderTx(ctx, orderId, logFields any) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "CancelOrderTx", reflect.TypeOf((*MockDBStore)(nil).CancelOrderTx), ctx, orderId, logFields)
}

// CreateOrder mocks base method.
func (m *MockDBStore) CreateOrder(ctx context.Context, arg db.CreateOrderParams) (db.Order, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "CreateOrder", ctx, arg)
	ret0, _ := ret[0].(db.Order)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// CreateOrder indicates an expected call of CreateOrder.
func (mr *MockDBStoreMockRecorder) CreateOrder(ctx, arg any) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "CreateOrder", reflect.TypeOf((*MockDBStore)(nil).CreateOrder), ctx, arg)
}

// GetOrder mocks base method.
func (m *MockDBStore) GetOrder(ctx context.Context, id int32) (db.Order, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "GetOrder", ctx, id)
	ret0, _ := ret[0].(db.Order)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// GetOrder indicates an expected call of GetOrder.
func (mr *MockDBStoreMockRecorder) GetOrder(ctx, id any) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "GetOrder", reflect.TypeOf((*MockDBStore)(nil).GetOrder), ctx, id)
}

// ListOrders mocks base method.
func (m *MockDBStore) ListOrders(ctx context.Context, arg db.ListOrdersParams) ([]db.Order, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "ListOrders", ctx, arg)
	ret0, _ := ret[0].([]db.Order)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// ListOrders indicates an expected call of ListOrders.
func (mr *MockDBStoreMockRecorder) ListOrders(ctx, arg any) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "ListOrders", reflect.TypeOf((*MockDBStore)(nil).ListOrders), ctx, arg)
}

// UpdateOrderStatus mocks base method.
func (m *MockDBStore) UpdateOrderStatus(ctx context.Context, arg db.UpdateOrderStatusParams) (db.Order, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UpdateOrderStatus", ctx, arg)
	ret0, _ := ret[0].(db.Order)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// UpdateOrderStatus indicates an expected call of UpdateOrderStatus.
func (mr *MockDBStoreMockRecorder) UpdateOrderStatus(ctx, arg any) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UpdateOrderStatus", reflect.TypeOf((*MockDBStore)(nil).UpdateOrderStatus), ctx, arg)
}

// ValidateAndUpdateOrderStatusTx mocks base method.
func (m *MockDBStore) ValidateAndUpdateOrderStatusTx(ctx context.Context, id int, expectedCurrentStatus, newStatus db.OrderStatus, logFields slog.Attr) (int, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "ValidateAndUpdateOrderStatusTx", ctx, id, expectedCurrentStatus, newStatus, logFields)
	ret0, _ := ret[0].(int)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// ValidateAndUpdateOrderStatusTx indicates an expected call of ValidateAndUpdateOrderStatusTx.
func (mr *MockDBStoreMockRecorder) ValidateAndUpdateOrderStatusTx(ctx, id, expectedCurrentStatus, newStatus, logFields any) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "ValidateAndUpdateOrderStatusTx", reflect.TypeOf((*MockDBStore)(nil).ValidateAndUpdateOrderStatusTx), ctx, id, expectedCurrentStatus, newStatus, logFields)
}
