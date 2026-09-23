package com.ledgerflow.query.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_audit_entries")
public class SettlementAuditEntry {

    @Id
    private UUID id;

    @Column(name = "settlement_id", nullable = false)
    private UUID settlementId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected SettlementAuditEntry() {
    }

    public SettlementAuditEntry(UUID id, UUID settlementId, String status, String failureReason, Instant occurredAt) {
        this.id = id;
        this.settlementId = settlementId;
        this.status = status;
        this.failureReason = failureReason;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public String getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
