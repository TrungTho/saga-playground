package com.saga.playground.checkoutservice.infrastructure.repositories;

import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionalInboxOrderRepository extends JpaRepository<TransactionalInboxOrder, Long> {

    // just keep it here for usage reference, will use delete all for this playground
    // @Query("DELETE FROM TransactionalInboxOrder WHERE orderId < 100")
    // void deleteTestData();

    Optional<TransactionalInboxOrder> findByOrderId(String orderId);

    void deleteByOrderId(String orderId);
    
}
