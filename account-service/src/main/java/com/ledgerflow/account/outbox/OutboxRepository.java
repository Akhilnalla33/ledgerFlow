package com.ledgerflow.account.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface OutboxRepository extends JpaRepository<OutboxEvent, java.util.UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OutboxEvent o where o.status = com.ledgerflow.account.outbox.OutboxStatus.PENDING "
            + "order by o.createdAt asc")
    List<OutboxEvent> findPendingBatchForUpdate(org.springframework.data.domain.Pageable pageable);
}
