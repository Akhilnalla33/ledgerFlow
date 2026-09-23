package com.ledgerflow.account.service;

import java.util.UUID;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(UUID fromAccountId, UUID toAccountId, String requested) {
        super("Currency " + requested + " does not match accounts " + fromAccountId + " -> " + toAccountId);
    }
}
