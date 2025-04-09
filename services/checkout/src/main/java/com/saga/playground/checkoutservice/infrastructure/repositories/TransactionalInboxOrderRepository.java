package com.saga.playground.checkoutservice.infrastructure.repositories;

import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionalInboxOrderRepository extends JpaRepository<TransactionalInboxOrder, Long> {

    // just keep it here for usage reference, will use delete all for this playground
    // @Query("DELETE FROM TransactionalInboxOrder WHERE orderId < 100")
    // void deleteTestData();

    Optional<TransactionalInboxOrder> findByOrderId(String orderId);

    void deleteByOrderId(String orderId);

    List<TransactionalInboxOrder> findByWorkerIdAndStatus(String workerId, InboxOrderStatus status);

    @Query(
        value = "SELECT * FROM checkout_schema.t_inbox_order t "
            + "WHERE (t.worker_id <> '') IS NOT TRUE "
            + "AND t.status = 'NEW'::inbox_order_status "
            + "LIMIT 10",
        nativeQuery = true)
    List<TransactionalInboxOrder> findNewOrders();

}
