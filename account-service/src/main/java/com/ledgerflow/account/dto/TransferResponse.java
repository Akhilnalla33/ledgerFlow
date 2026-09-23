package com.ledgerflow.account.dto;

import com.ledgerflow.account.domain.Transfer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getFromAccountId(), transfer.getToAccountId(),
                transfer.getAmount(), transfer.getCurrency(), transfer.getStatus().name(), transfer.getCreatedAt());
    }
}
