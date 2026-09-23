package com.ledgerflow.settlement.client;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Settlement orchestration genuinely needs a synchronous answer to "did this leg's transfer
 * succeed or fail" before deciding whether to proceed to the next step or start compensating —
 * that is why this is a REST call rather than an async Kafka round-trip (see ADR-0001).
 * {@code idempotencyKey} is always {@code <settlementId>-<stepOrder>[-compensate]} so retrying
 * a step (including a retry the orchestrator issues itself after a transient failure) can
 * never double-apply a transfer.
 */
public interface AccountServiceClient {

    TransferResult transfer(String idempotencyKey, UUID fromAccountId, UUID toAccountId, BigDecimal amount,
            String currency, String reason);

    record TransferResult(UUID transferId) {
    }

    class AccountServiceCallException extends RuntimeException {
        public AccountServiceCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
