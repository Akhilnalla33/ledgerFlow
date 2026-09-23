package com.ledgerflow.account.service;

/**
 * Result of {@link IdempotencyService#begin(String, String)}: either this caller won the race
 * to execute the request ({@link #firstAttempt()}), or the cached result of a prior/concurrent
 * execution should be returned as-is ({@link #cachedStatus()}, {@link #cachedBody()}).
 */
public final class IdempotencyOutcome {

    private final boolean firstAttempt;
    private final Integer cachedStatus;
    private final String cachedBody;

    private IdempotencyOutcome(boolean firstAttempt, Integer cachedStatus, String cachedBody) {
        this.firstAttempt = firstAttempt;
        this.cachedStatus = cachedStatus;
        this.cachedBody = cachedBody;
    }

    public static IdempotencyOutcome first() {
        return new IdempotencyOutcome(true, null, null);
    }

    public static IdempotencyOutcome replay(int status, String body) {
        return new IdempotencyOutcome(false, status, body);
    }

    public boolean firstAttempt() {
        return firstAttempt;
    }

    public Integer cachedStatus() {
        return cachedStatus;
    }

    public String cachedBody() {
        return cachedBody;
    }
}
