package com.ledgerflow.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by settlement-service as a settlement saga progresses. {@code status} follows the
 * saga state machine: STARTED -&gt; IN_PROGRESS -&gt; COMPLETED, or STARTED -&gt; IN_PROGRESS
 * -&gt; COMPENSATING -&gt; COMPENSATED on failure.
 */
public class SettlementStatusChangedEvent extends BaseEvent {

    private UUID settlementId;
    private String status;
    private String failureReason;
    private List<UUID> participantAccountIds;

    public SettlementStatusChangedEvent() {
        super();
    }

    public SettlementStatusChangedEvent(UUID eventId, Instant occurredAt, String correlationId,
            UUID settlementId, String status, String failureReason,
            List<UUID> participantAccountIds) {
        super(eventId, occurredAt, correlationId, 1);
        this.settlementId = settlementId;
        this.status = status;
        this.failureReason = failureReason;
        this.participantAccountIds = participantAccountIds;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(UUID settlementId) {
        this.settlementId = settlementId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public List<UUID> getParticipantAccountIds() {
        return participantAccountIds;
    }

    public void setParticipantAccountIds(List<UUID> participantAccountIds) {
        this.participantAccountIds = participantAccountIds;
    }
}
