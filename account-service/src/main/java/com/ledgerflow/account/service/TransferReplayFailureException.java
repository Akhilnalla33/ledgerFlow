package com.ledgerflow.account.service;

/** A prior attempt using this Idempotency-Key failed; replay the same failure verbatim. */
public class TransferReplayFailureException extends RuntimeException {

    public TransferReplayFailureException(String originalFailureMessage) {
        super(originalFailureMessage);
    }
}
