package com.saga.playground.checkoutservice.domains.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "checkout_schema")
@ToString
public class Checkout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String userId;

    private String checkoutSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    @JdbcType(value = PostgreSQLEnumJdbcType.class) // this will help to convert from enum of Java <-> enum of Postgres
    private PaymentStatus checkoutStatus;

    @Column(nullable = false)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.JSON)
    private String webhookPayload;

    @Column(nullable = false)
    private Boolean eventPublished = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Checkout(String orderId, String userId, PaymentStatus status, BigDecimal amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.checkoutStatus = status;
        this.amount = amount;
    }
}
