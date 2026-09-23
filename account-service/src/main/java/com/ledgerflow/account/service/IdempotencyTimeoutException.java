package com.ledgerflow.account.service;

public class IdempotencyTimeoutException extends RuntimeException {

    public IdempotencyTimeoutException(String key) {
        super("Timed out waiting for the in-flight request holding Idempotency-Key " + key);
    }
}
