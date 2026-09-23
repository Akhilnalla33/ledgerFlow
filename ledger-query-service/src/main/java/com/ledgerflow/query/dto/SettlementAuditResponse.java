package com.ledgerflow.query.dto;

import com.ledgerflow.query.domain.SettlementAuditEntry;
import java.time.Instant;

public record SettlementAuditResponse(String status, String failureReason, Instant occurredAt) {

    public static SettlementAuditResponse from(SettlementAuditEntry entry) {
        return new SettlementAuditResponse(entry.getStatus(), entry.getFailureReason(), entry.getOccurredAt());
    }
}
