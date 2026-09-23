package com.ledgerflow.account.web.error;

public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException() {
        super("Idempotency-Key header is required on this endpoint");
    }
}
