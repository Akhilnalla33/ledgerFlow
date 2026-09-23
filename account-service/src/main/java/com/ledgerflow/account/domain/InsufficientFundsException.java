package com.ledgerflow.account.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    private final UUID accountId;

    public InsufficientFundsException(UUID accountId, BigDecimal available, BigDecimal requested) {
        super("Account " + accountId + " has insufficient funds: available=" + available
                + " requested=" + requested);
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
