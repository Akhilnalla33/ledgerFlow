package com.ledgerflow.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ledgerflow.events.AccountBalanceChangedEvent;
import com.ledgerflow.query.domain.BalanceSnapshot;
import com.ledgerflow.query.repository.BalanceSnapshotRepository;
import com.ledgerflow.query.repository.StatementEntryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountBalanceProjectorTest {

    @Mock
    private BalanceSnapshotRepository balanceSnapshotRepository;
    @Mock
    private StatementEntryRepository statementEntryRepository;

    @Test
    void appliesEventAndUpsertsSnapshot() {
        UUID accountId = UUID.randomUUID();
        UUID ledgerEntryId = UUID.randomUUID();
        AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(UUID.randomUUID(), Instant.now(),
                "corr-1", accountId, UUID.randomUUID(), ledgerEntryId, "CREDIT", new BigDecimal("10.00"), "USD",
                new BigDecimal("110.00"), "test");

        when(statementEntryRepository.existsById(ledgerEntryId)).thenReturn(false);
        when(balanceSnapshotRepository.findById(accountId)).thenReturn(Optional.empty());

        AccountBalanceProjector projector = new AccountBalanceProjector(balanceSnapshotRepository,
                statementEntryRepository);
        projector.apply(event);

        verify(balanceSnapshotRepository).save(org.mockito.ArgumentMatchers.argThat(
                (BalanceSnapshot s) -> s.getBalance().compareTo(new BigDecimal("110.00")) == 0));
        verify(statementEntryRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reprocessingTheSameLedgerEntryIdIsANoOp() {
        UUID accountId = UUID.randomUUID();
        UUID ledgerEntryId = UUID.randomUUID();
        AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(UUID.randomUUID(), Instant.now(),
                "corr-1", accountId, UUID.randomUUID(), ledgerEntryId, "CREDIT", new BigDecimal("10.00"), "USD",
                new BigDecimal("110.00"), "test");

        when(statementEntryRepository.existsById(ledgerEntryId)).thenReturn(true);

        AccountBalanceProjector projector = new AccountBalanceProjector(balanceSnapshotRepository,
                statementEntryRepository);
        projector.apply(event);

        verify(balanceSnapshotRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(statementEntryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
