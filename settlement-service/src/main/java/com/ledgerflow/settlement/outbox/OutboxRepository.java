package com.ledgerflow.settlement.outbox;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OutboxEvent o where o.status = com.ledgerflow.settlement.outbox.OutboxStatus.PENDING "
            + "order by o.id asc")
    List<OutboxEvent> findPendingBatchForUpdate(Pageable pageable);
}
