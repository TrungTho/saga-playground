// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: order.proto
// Protobuf Java Version: 4.29.3

package com.saga.playground.checkoutservice.grpc.protobufs;

public interface OrderOrBuilder extends
    // @@protoc_insertion_point(interface_extends:Order)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * order id
   * </pre>
   *
   * <code>int32 id = 1;</code>
   * @return The id.
   */
  int getId();

  /**
   * <pre>
   * order status
   * </pre>
   *
   * <code>string status = 2;</code>
   * @return The status.
   */
  java.lang.String getStatus();
  /**
   * <pre>
   * order status
   * </pre>
   *
   * <code>string status = 2;</code>
   * @return The bytes for status.
   */
  com.google.protobuf.ByteString
      getStatusBytes();
}
