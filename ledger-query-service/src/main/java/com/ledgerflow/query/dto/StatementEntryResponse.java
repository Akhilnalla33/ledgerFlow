package com.ledgerflow.query.dto;

import com.ledgerflow.query.domain.StatementEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StatementEntryResponse(
        UUID id, UUID transferId, String direction, BigDecimal amount, String currency,
        BigDecimal resultingBalance, String reason, Instant occurredAt) {

    public static StatementEntryResponse from(StatementEntry entry) {
        return new StatementEntryResponse(entry.getId(), entry.getTransferId(), entry.getDirection(),
                entry.getAmount(), entry.getCurrency(), entry.getResultingBalance(), entry.getReason(),
                entry.getOccurredAt());
    }
}
