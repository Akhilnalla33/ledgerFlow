package com.ledgerflow.account.dto;

import com.ledgerflow.account.domain.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id, UUID accountId, UUID transferId, String entryType, BigDecimal amount,
        String currency, BigDecimal balanceAfter, String description, Instant createdAt) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(entry.getId(), entry.getAccountId(), entry.getTransferId(),
                entry.getEntryType().name(), entry.getAmount(), entry.getCurrency(),
                entry.getBalanceAfter(), entry.getDescription(), entry.getCreatedAt());
    }
}
