package com.ledgerflow.account.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ledgerflow.account.domain.IdempotencyKeyRecord;
import com.ledgerflow.account.service.IdempotencyConflictException;
import com.ledgerflow.account.service.IdempotencyKeyStore;
import com.ledgerflow.account.service.IdempotencyOutcome;
import com.ledgerflow.account.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyStore store;

    @Test
    void firstCallerWinsAndGetsFirstAttemptOutcome() {
        when(store.tryInsert("key-1", "hash-1")).thenReturn(true);

        IdempotencyService service = new IdempotencyService(store);
        IdempotencyOutcome outcome = service.begin("key-1", "hash-1");

        assertThat(outcome.firstAttempt()).isTrue();
    }

    @Test
    void loserGetsTheWinnersCompletedResponse() {
        when(store.tryInsert("key-2", "hash-2")).thenReturn(false);
        IdempotencyKeyRecord completed = new IdempotencyKeyRecord("key-2", "hash-2");
        completed.complete(201, "{\"transferId\":\"abc\"}");
        when(store.readCommitted("key-2")).thenReturn(completed);

        IdempotencyService service = new IdempotencyService(store);
        IdempotencyOutcome outcome = service.begin("key-2", "hash-2");

        assertThat(outcome.firstAttempt()).isFalse();
        assertThat(outcome.cachedStatus()).isEqualTo(201);
        assertThat(outcome.cachedBody()).isEqualTo("{\"transferId\":\"abc\"}");
    }

    @Test
    void sameKeyDifferentPayloadIsRejectedAsConflict() {
        when(store.tryInsert("key-3", "hash-new")).thenReturn(false);
        IdempotencyKeyRecord existing = new IdempotencyKeyRecord("key-3", "hash-original");
        existing.complete(201, "{}");
        when(store.readCommitted("key-3")).thenReturn(existing);

        IdempotencyService service = new IdempotencyService(store);

        org.junit.jupiter.api.Assertions.assertThrows(IdempotencyConflictException.class,
                () -> service.begin("key-3", "hash-new"));
    }

    @Test
    void completeDelegatesToStore() {
        IdempotencyService service = new IdempotencyService(store);
        service.complete("key-4", 201, "body");
        verify(store).complete("key-4", 201, "body");
    }
}
