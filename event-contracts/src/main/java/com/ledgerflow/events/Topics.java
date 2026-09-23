package com.ledgerflow.events;

/** Canonical Kafka topic names shared by every LedgerFlow service. */
public final class Topics {

    public static final String ACCOUNT_BALANCE_CHANGED = "account.balance-changed.v1";
    public static final String SETTLEMENT_STATUS_CHANGED = "settlement.status-changed.v1";

    private Topics() {
    }
}
