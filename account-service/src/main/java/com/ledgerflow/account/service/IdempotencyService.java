package com.ledgerflow.account.service;

import com.ledgerflow.account.domain.IdempotencyKeyRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;

/**
 * Enforces "replay the same {@code Idempotency-Key} and get the original result, never a
 * second execution." The {@code idempotency_keys} table's primary key does the real work:
 * {@link #begin} tries an INSERT in its own committed transaction (via {@link IdempotencyKeyStore}),
 * so when N concurrent requests race on the same key, the database's unique-constraint check —
 * not application code — decides exactly one winner. Every loser blocks in
 * {@link #awaitCompletion} until the winner commits its response, then returns that exact
 * response instead of re-running the transfer.
 */
@Service
public class IdempotencyService {

    private static final int POLL_INTERVAL_MS = 25;
    private static final int MAX_WAIT_MS = 10_000;

    private final IdempotencyKeyStore store;

    public IdempotencyService(IdempotencyKeyStore store) {
        this.store = store;
    }

    public static String hash(String requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Claims the key for this caller, or waits for and returns a concurrent/prior result. */
    public IdempotencyOutcome begin(String key, String requestHash) {
        boolean won = store.tryInsert(key, requestHash);
        if (won) {
            return IdempotencyOutcome.first();
        }
        return awaitCompletion(key, requestHash);
    }

    private IdempotencyOutcome awaitCompletion(String key, String requestHash) {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            IdempotencyKeyRecord record = store.readCommitted(key);
            if (record != null) {
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException(key);
                }
                if (!record.isInProgress()) {
                    return IdempotencyOutcome.replay(record.getResponseStatus(), record.getResponseBody());
                }
            }
            sleep();
        }
        throw new IdempotencyTimeoutException(key);
    }

    public void complete(String key, int status, String body) {
        store.complete(key, status, body);
    }

    private void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for idempotent result", e);
        }
    }
}
