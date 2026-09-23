package com.ledgerflow.account.service;

import com.ledgerflow.account.domain.IdempotencyKeyRecord;
import com.ledgerflow.account.repository.IdempotencyKeyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Separated from {@link IdempotencyService} so every method here goes through Spring's
 * transactional proxy: each call commits (or rolls back) on its own as soon as it returns,
 * independent of any transaction the caller might be in. Merging this back into
 * {@code IdempotencyService} would make the {@code REQUIRES_NEW} calls self-invocations that
 * Spring's proxy-based AOP silently ignores — a classic footgun this class exists to avoid.
 */
@Component
public class IdempotencyKeyStore {

    private final IdempotencyKeyRepository repository;

    public IdempotencyKeyStore(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryInsert(String key, String requestHash) {
        try {
            repository.saveAndFlush(new IdempotencyKeyRecord(key, requestHash));
            return true;
        } catch (DataIntegrityViolationException alreadyClaimed) {
            // On a real database (Postgres), a failed statement poisons the rest of this
            // transaction until it is rolled back. Marking rollback-only here means the
            // enclosing @Transactional commits nothing instead of trying (and failing) to
            // COMMIT an aborted transaction, while this method still returns normally.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public IdempotencyKeyRecord readCommitted(String key) {
        return repository.findByIdempotencyKey(key).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, int status, String body) {
        IdempotencyKeyRecord record = repository.findByIdempotencyKey(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency key vanished: " + key));
        record.complete(status, body);
        repository.save(record);
    }
}
