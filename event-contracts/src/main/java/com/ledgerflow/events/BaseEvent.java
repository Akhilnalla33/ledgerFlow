package com.ledgerflow.events;

import java.time.Instant;
import java.util.UUID;

/** Common envelope fields every LedgerFlow domain event carries. */
public abstract class BaseEvent {

    private UUID eventId;
    private Instant occurredAt;
    private String correlationId;
    private int schemaVersion;

    protected BaseEvent() {
    }

    protected BaseEvent(UUID eventId, Instant occurredAt, String correlationId, int schemaVersion) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.schemaVersion = schemaVersion;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
