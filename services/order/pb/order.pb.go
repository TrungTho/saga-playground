// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.36.5
// 	protoc        v5.29.3
// source: order.proto

package pb

import (
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
	sync "sync"
	unsafe "unsafe"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

type Order struct {
	state         protoimpl.MessageState `protogen:"open.v1"`
	Id            int32                  `protobuf:"varint,1,opt,name=id,proto3" json:"id,omitempty"`        // order id
	Status        string                 `protobuf:"bytes,2,opt,name=status,proto3" json:"status,omitempty"` //order status
	unknownFields protoimpl.UnknownFields
	sizeCache     protoimpl.SizeCache
}

func (x *Order) Reset() {
	*x = Order{}
	mi := &file_order_proto_msgTypes[0]
	ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
	ms.StoreMessageInfo(mi)
}

func (x *Order) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Order) ProtoMessage() {}

func (x *Order) ProtoReflect() protoreflect.Message {
	mi := &file_order_proto_msgTypes[0]
	if x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Order.ProtoReflect.Descriptor instead.
func (*Order) Descriptor() ([]byte, []int) {
	return file_order_proto_rawDescGZIP(), []int{0}
}

func (x *Order) GetId() int32 {
	if x != nil {
		return x.Id
	}
	return 0
}

func (x *Order) GetStatus() string {
	if x != nil {
		return x.Status
	}
	return ""
}

// request to swich order status to OrderStatusPendingPayment and prevent any further cancelling request from user
type SwitchToPendingPaymentRequest struct {
	state         protoimpl.MessageState `protogen:"open.v1"`
	Id            int32                  `protobuf:"varint,1,opt,name=id,proto3" json:"id,omitempty"` // order id
	unknownFields protoimpl.UnknownFields
	sizeCache     protoimpl.SizeCache
}

func (x *SwitchToPendingPaymentRequest) Reset() {
	*x = SwitchToPendingPaymentRequest{}
	mi := &file_order_proto_msgTypes[1]
	ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
	ms.StoreMessageInfo(mi)
}

func (x *SwitchToPendingPaymentRequest) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*SwitchToPendingPaymentRequest) ProtoMessage() {}

func (x *SwitchToPendingPaymentRequest) ProtoReflect() protoreflect.Message {
	mi := &file_order_proto_msgTypes[1]
	if x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use SwitchToPendingPaymentRequest.ProtoReflect.Descriptor instead.
func (*SwitchToPendingPaymentRequest) Descriptor() ([]byte, []int) {
	return file_order_proto_rawDescGZIP(), []int{1}
}

func (x *SwitchToPendingPaymentRequest) GetId() int32 {
	if x != nil {
		return x.Id
	}
	return 0
}

type SwitchToPendingPaymentResponse struct {
	state         protoimpl.MessageState `protogen:"open.v1"`
	Order         *Order                 `protobuf:"bytes,1,opt,name=order,proto3" json:"order,omitempty"` // return the updated order
	unknownFields protoimpl.UnknownFields
	sizeCache     protoimpl.SizeCache
}

func (x *SwitchToPendingPaymentResponse) Reset() {
	*x = SwitchToPendingPaymentResponse{}
	mi := &file_order_proto_msgTypes[2]
	ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
	ms.StoreMessageInfo(mi)
}

func (x *SwitchToPendingPaymentResponse) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*SwitchToPendingPaymentResponse) ProtoMessage() {}

func (x *SwitchToPendingPaymentResponse) ProtoReflect() protoreflect.Message {
	mi := &file_order_proto_msgTypes[2]
	if x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use SwitchToPendingPaymentResponse.ProtoReflect.Descriptor instead.
func (*SwitchToPendingPaymentResponse) Descriptor() ([]byte, []int) {
	return file_order_proto_rawDescGZIP(), []int{2}
}

func (x *SwitchToPendingPaymentResponse) GetOrder() *Order {
	if x != nil {
		return x.Order
	}
	return nil
}

var File_order_proto protoreflect.FileDescriptor

var file_order_proto_rawDesc = string([]byte{
	0x0a, 0x0b, 0x6f, 0x72, 0x64, 0x65, 0x72, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x22, 0x2f, 0x0a,
	0x05, 0x4f, 0x72, 0x64, 0x65, 0x72, 0x12, 0x0e, 0x0a, 0x02, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01,
	0x28, 0x05, 0x52, 0x02, 0x69, 0x64, 0x12, 0x16, 0x0a, 0x06, 0x73, 0x74, 0x61, 0x74, 0x75, 0x73,
	0x18, 0x02, 0x20, 0x01, 0x28, 0x09, 0x52, 0x06, 0x73, 0x74, 0x61, 0x74, 0x75, 0x73, 0x22, 0x2f,
	0x0a, 0x1d, 0x53, 0x77, 0x69, 0x74, 0x63, 0x68, 0x54, 0x6f, 0x50, 0x65, 0x6e, 0x64, 0x69, 0x6e,
	0x67, 0x50, 0x61, 0x79, 0x6d, 0x65, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x12,
	0x0e, 0x0a, 0x02, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x05, 0x52, 0x02, 0x69, 0x64, 0x22,
	0x3e, 0x0a, 0x1e, 0x53, 0x77, 0x69, 0x74, 0x63, 0x68, 0x54, 0x6f, 0x50, 0x65, 0x6e, 0x64, 0x69,
	0x6e, 0x67, 0x50, 0x61, 0x79, 0x6d, 0x65, 0x6e, 0x74, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73,
	0x65, 0x12, 0x1c, 0x0a, 0x05, 0x6f, 0x72, 0x64, 0x65, 0x72, 0x18, 0x01, 0x20, 0x01, 0x28, 0x0b,
	0x32, 0x06, 0x2e, 0x4f, 0x72, 0x64, 0x65, 0x72, 0x52, 0x05, 0x6f, 0x72, 0x64, 0x65, 0x72, 0x32,
	0x6e, 0x0a, 0x0c, 0x4f, 0x72, 0x64, 0x65, 0x72, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x12,
	0x5e, 0x0a, 0x1b, 0x53, 0x77, 0x69, 0x74, 0x63, 0x68, 0x4f, 0x72, 0x64, 0x65, 0x72, 0x54, 0x6f,
	0x50, 0x65, 0x6e, 0x64, 0x69, 0x6e, 0x67, 0x50, 0x61, 0x79, 0x6d, 0x65, 0x6e, 0x74, 0x12, 0x1e,
	0x2e, 0x53, 0x77, 0x69, 0x74, 0x63, 0x68, 0x54, 0x6f, 0x50, 0x65, 0x6e, 0x64, 0x69, 0x6e, 0x67,
	0x50, 0x61, 0x79, 0x6d, 0x65, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x1a, 0x1f,
	0x2e, 0x53, 0x77, 0x69, 0x74, 0x63, 0x68, 0x54, 0x6f, 0x50, 0x65, 0x6e, 0x64, 0x69, 0x6e, 0x67,
	0x50, 0x61, 0x79, 0x6d, 0x65, 0x6e, 0x74, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x42,
	0x3c, 0x0a, 0x32, 0x63, 0x6f, 0x6d, 0x2e, 0x73, 0x61, 0x67, 0x61, 0x2e, 0x70, 0x6c, 0x61, 0x79,
	0x67, 0x72, 0x6f, 0x75, 0x6e, 0x64, 0x2e, 0x63, 0x68, 0x65, 0x63, 0x6b, 0x6f, 0x75, 0x74, 0x73,
	0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x67, 0x72, 0x70, 0x63, 0x2e, 0x70, 0x72, 0x6f, 0x74,
	0x6f, 0x62, 0x75, 0x66, 0x73, 0x50, 0x01, 0x5a, 0x04, 0x2e, 0x2f, 0x70, 0x62, 0x62, 0x06, 0x70,
	0x72, 0x6f, 0x74, 0x6f, 0x33,
})

var (
	file_order_proto_rawDescOnce sync.Once
	file_order_proto_rawDescData []byte
)

func file_order_proto_rawDescGZIP() []byte {
	file_order_proto_rawDescOnce.Do(func() {
		file_order_proto_rawDescData = protoimpl.X.CompressGZIP(unsafe.Slice(unsafe.StringData(file_order_proto_rawDesc), len(file_order_proto_rawDesc)))
	})
	return file_order_proto_rawDescData
}

var file_order_proto_msgTypes = make([]protoimpl.MessageInfo, 3)
var file_order_proto_goTypes = []any{
	(*Order)(nil),                          // 0: Order
	(*SwitchToPendingPaymentRequest)(nil),  // 1: SwitchToPendingPaymentRequest
	(*SwitchToPendingPaymentResponse)(nil), // 2: SwitchToPendingPaymentResponse
}
var file_order_proto_depIdxs = []int32{
	0, // 0: SwitchToPendingPaymentResponse.order:type_name -> Order
	1, // 1: OrderService.SwitchOrderToPendingPayment:input_type -> SwitchToPendingPaymentRequest
	2, // 2: OrderService.SwitchOrderToPendingPayment:output_type -> SwitchToPendingPaymentResponse
	2, // [2:3] is the sub-list for method output_type
	1, // [1:2] is the sub-list for method input_type
	1, // [1:1] is the sub-list for extension type_name
	1, // [1:1] is the sub-list for extension extendee
	0, // [0:1] is the sub-list for field type_name
}

func init() { file_order_proto_init() }
func file_order_proto_init() {
	if File_order_proto != nil {
		return
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: unsafe.Slice(unsafe.StringData(file_order_proto_rawDesc), len(file_order_proto_rawDesc)),
			NumEnums:      0,
			NumMessages:   3,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_order_proto_goTypes,
		DependencyIndexes: file_order_proto_depIdxs,
		MessageInfos:      file_order_proto_msgTypes,
	}.Build()
	File_order_proto = out.File
	file_order_proto_goTypes = nil
	file_order_proto_depIdxs = nil
}
