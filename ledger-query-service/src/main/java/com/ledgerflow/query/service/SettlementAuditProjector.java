package com.ledgerflow.query.service;

import com.ledgerflow.events.SettlementStatusChangedEvent;
import com.ledgerflow.query.domain.SettlementAuditEntry;
import com.ledgerflow.query.repository.SettlementAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementAuditProjector {

    private final SettlementAuditRepository repository;

    public SettlementAuditProjector(SettlementAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void apply(SettlementStatusChangedEvent event) {
        if (repository.existsById(event.getEventId())) {
            return;
        }
        repository.save(new SettlementAuditEntry(event.getEventId(), event.getSettlementId(), event.getStatus(),
                event.getFailureReason(), event.getOccurredAt()));
    }
}
