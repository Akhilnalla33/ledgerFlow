package com.ledgerflow.query.service;

import com.ledgerflow.events.AccountBalanceChangedEvent;
import com.ledgerflow.query.domain.BalanceSnapshot;
import com.ledgerflow.query.domain.StatementEntry;
import com.ledgerflow.query.repository.BalanceSnapshotRepository;
import com.ledgerflow.query.repository.StatementEntryRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies one {@link AccountBalanceChangedEvent} to the read model: upserts the account's
 * latest {@link BalanceSnapshot} and appends an immutable {@link StatementEntry}. Idempotent
 * by {@code ledgerEntryId} (the statement entry's primary key), so Kafka's at-least-once
 * delivery re-processing the same event twice is a harmless no-op rather than a double count.
 */
@Service
public class AccountBalanceProjector {

    private final BalanceSnapshotRepository balanceSnapshotRepository;
    private final StatementEntryRepository statementEntryRepository;

    public AccountBalanceProjector(BalanceSnapshotRepository balanceSnapshotRepository,
            StatementEntryRepository statementEntryRepository) {
        this.balanceSnapshotRepository = balanceSnapshotRepository;
        this.statementEntryRepository = statementEntryRepository;
    }

    @Transactional
    public void apply(AccountBalanceChangedEvent event) {
        UUID entryId = event.getLedgerEntryId();
        if (statementEntryRepository.existsById(entryId)) {
            return;
        }

        BalanceSnapshot snapshot = balanceSnapshotRepository.findById(event.getAccountId())
                .orElseGet(() -> new BalanceSnapshot(event.getAccountId(), event.getResultingBalance(),
                        event.getCurrency(), event.getOccurredAt()));
        snapshot.apply(event.getResultingBalance(), event.getOccurredAt());
        balanceSnapshotRepository.save(snapshot);

        statementEntryRepository.save(new StatementEntry(entryId, event.getAccountId(), event.getTransferId(),
                event.getDirection(), event.getAmount(), event.getCurrency(), event.getResultingBalance(),
                event.getReason(), event.getOccurredAt()));
    }
}
