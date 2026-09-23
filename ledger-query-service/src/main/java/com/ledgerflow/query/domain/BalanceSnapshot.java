package com.ledgerflow.query.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "balance_snapshots")
public class BalanceSnapshot {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BalanceSnapshot() {
    }

    public BalanceSnapshot(UUID accountId, BigDecimal balance, String currency, Instant updatedAt) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }

    public void apply(BigDecimal balance, Instant updatedAt) {
        this.balance = balance;
        this.updatedAt = updatedAt;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
