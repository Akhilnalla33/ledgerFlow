package com.ledgerflow.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by account-service, via the transactional outbox, every time a transfer posts
 * balanced ledger entries against an account. One transfer produces exactly two of these
 * events: one for the debited account, one for the credited account.
 */
public class AccountBalanceChangedEvent extends BaseEvent {

    private UUID accountId;
    private UUID transferId;
    private UUID ledgerEntryId;
    private String direction; // DEBIT or CREDIT
    private BigDecimal amount;
    private String currency;
    private BigDecimal resultingBalance;
    private String reason;

    public AccountBalanceChangedEvent() {
        super();
    }

    public AccountBalanceChangedEvent(UUID eventId, Instant occurredAt, String correlationId,
            UUID accountId, UUID transferId, UUID ledgerEntryId, String direction,
            BigDecimal amount, String currency, BigDecimal resultingBalance, String reason) {
        super(eventId, occurredAt, correlationId, 1);
        this.accountId = accountId;
        this.transferId = transferId;
        this.ledgerEntryId = ledgerEntryId;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.resultingBalance = resultingBalance;
        this.reason = reason;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public void setTransferId(UUID transferId) {
        this.transferId = transferId;
    }

    public UUID getLedgerEntryId() {
        return ledgerEntryId;
    }

    public void setLedgerEntryId(UUID ledgerEntryId) {
        this.ledgerEntryId = ledgerEntryId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public void setResultingBalance(BigDecimal resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
