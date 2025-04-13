package com.saga.playground.checkoutservice.domains.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table
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
    @JdbcTypeCode(SqlTypes.ENUM)
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
