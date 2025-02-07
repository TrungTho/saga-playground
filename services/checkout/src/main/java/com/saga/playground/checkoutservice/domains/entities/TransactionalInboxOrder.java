package com.saga.playground.checkoutservice.domains.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_inbox_order")
public class TransactionalInboxOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public TransactionalInboxOrder(String orderId, String payload, Instant createdAt, Instant updatedAt) {
        this.orderId = orderId;
        this.payload = payload;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
