package com.saga.playground.checkoutservice.infrastructure.repositories;

import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionalInboxOrderRepository extends JpaRepository<TransactionalInboxOrder, Long> {
}
