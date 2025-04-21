package com.saga.playground.checkoutservice.domains.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_inbox_order", schema = "checkout_schema")
@ToString
public class TransactionalInboxOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    @JdbcType(value = PostgreSQLEnumJdbcType.class) // this will help to convert from enum of Java <-> enum of Postgres
    private InboxOrderStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    private String workerId;

    private String note;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public TransactionalInboxOrder(String orderId, String payload) {
        this.orderId = orderId;
        this.payload = payload;
        this.status = InboxOrderStatus.NEW;
    }

}
