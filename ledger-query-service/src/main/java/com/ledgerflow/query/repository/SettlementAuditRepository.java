package com.ledgerflow.query.repository;

import com.ledgerflow.query.domain.SettlementAuditEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementAuditRepository extends JpaRepository<SettlementAuditEntry, UUID> {

    List<SettlementAuditEntry> findBySettlementIdOrderByOccurredAtAsc(UUID settlementId);
}
