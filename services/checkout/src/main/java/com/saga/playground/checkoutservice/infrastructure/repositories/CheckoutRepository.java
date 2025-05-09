package com.saga.playground.checkoutservice.infrastructure.repositories;

import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckoutRepository extends JpaRepository<Checkout, Long> {

    Optional<Checkout> findByOrderId(String orderId);

    List<Checkout> findTop100ByCheckoutStatusAndEventPublished(PaymentStatus status, Boolean isPublished);

}
