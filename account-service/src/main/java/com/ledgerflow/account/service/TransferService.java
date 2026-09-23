package com.ledgerflow.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.account.domain.Transfer;
import com.ledgerflow.account.dto.TransferRequest;
import com.ledgerflow.account.dto.TransferResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one transfer request: idempotency dedup, then optimistic-lock retry around
 * {@link LedgerService#executeTransfer}. This class deliberately holds no long-lived
 * transaction of its own — each retry attempt gets a fresh {@code REQUIRES_NEW} transaction
 * in {@link LedgerService}, so a losing attempt's partial work is rolled back cleanly before
 * the next attempt reloads the accounts at their latest committed version. See ADR-0002 for
 * why this project retries on {@link ObjectOptimisticLockingFailureException} instead of
 * serializing writers with {@code SELECT ... FOR UPDATE}.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final int MAX_ATTEMPTS = 10;

    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public TransferService(LedgerService ledgerService, IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    public TransferResponse transfer(String idempotencyKey, TransferRequest request, String correlationId) {
        String requestHash = IdempotencyService.hash(serialize(request));
        IdempotencyOutcome outcome = idempotencyService.begin(idempotencyKey, requestHash);
        if (!outcome.firstAttempt()) {
            if (outcome.cachedStatus() != HttpStatus.CREATED.value()) {
                throw new TransferReplayFailureException(outcome.cachedBody());
            }
            return deserialize(outcome.cachedBody());
        }

        try {
            TransferResponse response = executeWithRetry(request, correlationId);
            idempotencyService.complete(idempotencyKey, HttpStatus.CREATED.value(), serialize(response));
            return response;
        } catch (RuntimeException ex) {
            // A failed transfer is itself a definitive, replayable outcome: record it so a
            // retried request with the same key gets the same error instead of trying again
            // and potentially succeeding after the caller already gave up.
            idempotencyService.complete(idempotencyKey, HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage());
            throw ex;
        }
    }

    private TransferResponse executeWithRetry(TransferRequest request, String correlationId) {
        UUID transferId = UUID.randomUUID();
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                Transfer transfer = ledgerService.executeTransfer(transferId, request.fromAccountId(),
                        request.toAccountId(), request.amount(), request.currency(), request.reason(),
                        correlationId);
                return TransferResponse.from(transfer);
            } catch (ObjectOptimisticLockingFailureException conflict) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw conflict;
                }
                log.debug("Optimistic lock conflict on transfer {}, retrying (attempt {})", transferId, attempt);
            }
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize", ex);
        }
    }

    private TransferResponse deserialize(String body) {
        try {
            return objectMapper.readValue(body, TransferResponse.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize cached idempotent response", ex);
        }
    }
}
