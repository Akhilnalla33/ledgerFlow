package com.ledgerflow.query.dto;

import com.ledgerflow.query.domain.BalanceSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceSnapshotResponse(UUID accountId, BigDecimal balance, String currency, Instant updatedAt) {

    public static BalanceSnapshotResponse from(BalanceSnapshot snapshot) {
        return new BalanceSnapshotResponse(snapshot.getAccountId(), snapshot.getBalance(), snapshot.getCurrency(),
                snapshot.getUpdatedAt());
    }
}
