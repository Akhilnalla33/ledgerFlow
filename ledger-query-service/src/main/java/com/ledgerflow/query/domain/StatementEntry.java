package com.ledgerflow.query.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statement_entries")
public class StatementEntry {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(nullable = false, length = 6)
    private String direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "resulting_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal resultingBalance;

    @Column(length = 256)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected StatementEntry() {
    }

    public StatementEntry(UUID id, UUID accountId, UUID transferId, String direction, BigDecimal amount,
            String currency, BigDecimal resultingBalance, String reason, Instant occurredAt) {
        this.id = id;
        this.accountId = accountId;
        this.transferId = transferId;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.resultingBalance = resultingBalance;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public String getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
