package com.ledgerflow.account.service;

import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.domain.EntryType;
import com.ledgerflow.account.domain.LedgerEntry;
import com.ledgerflow.account.domain.Transfer;
import com.ledgerflow.account.domain.TransferStatus;
import com.ledgerflow.account.outbox.OutboxEvent;
import com.ledgerflow.account.outbox.OutboxRepository;
import com.ledgerflow.account.repository.AccountRepository;
import com.ledgerflow.account.repository.LedgerEntryRepository;
import com.ledgerflow.account.repository.TransferRepository;
import com.ledgerflow.events.AccountBalanceChangedEvent;
import com.ledgerflow.events.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes exactly one attempt at posting a balanced transfer. Every mutation here —
 * both account balance updates, both ledger entries, both outbox events, and the transfer
 * row — commits in a single local database transaction, so a caller either sees the whole
 * transfer applied or none of it. Optimistic-lock conflicts on the {@code accounts} row
 * surface as {@link org.springframework.orm.ObjectOptimisticLockingFailureException} and are
 * retried by {@link TransferService}, never swallowed here.
 */
@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransferRepository transferRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public LedgerService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository,
            TransferRepository transferRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transferRepository = transferRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transfer executeTransfer(UUID transferId, UUID fromAccountId, UUID toAccountId,
            BigDecimal amount, String currency, String reason, String correlationId) {
        // Lock/load accounts in a fixed global order (by id) regardless of debit/credit
        // direction so two transfers moving money in opposite directions between the same
        // pair of accounts can never deadlock waiting on each other's row.
        List<UUID> ordered = List.of(fromAccountId, toAccountId).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        Account first = accountRepository.findById(ordered.get(0))
                .orElseThrow(() -> new AccountNotFoundException(ordered.get(0)));
        Account second = accountRepository.findById(ordered.get(1))
                .orElseThrow(() -> new AccountNotFoundException(ordered.get(1)));

        Account from = fromAccountId.equals(first.getId()) ? first : second;
        Account to = fromAccountId.equals(first.getId()) ? second : first;

        if (!from.getCurrency().equals(currency) || !to.getCurrency().equals(currency)) {
            throw new CurrencyMismatchException(from.getId(), to.getId(), currency);
        }

        from.debit(amount);
        to.credit(amount);
        accountRepository.save(from);
        accountRepository.save(to);

        UUID debitEntryId = UUID.randomUUID();
        UUID creditEntryId = UUID.randomUUID();
        LedgerEntry debitEntry = new LedgerEntry(debitEntryId, from.getId(), transferId,
                EntryType.DEBIT, amount, currency, from.getBalance(), reason);
        LedgerEntry creditEntry = new LedgerEntry(creditEntryId, to.getId(), transferId,
                EntryType.CREDIT, amount, currency, to.getBalance(), reason);
        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        Transfer transfer = new Transfer(transferId, from.getId(), to.getId(), amount, currency,
                TransferStatus.COMPLETED, reason);
        transferRepository.save(transfer);

        writeOutboxEvent(from.getId(), transferId, debitEntryId, "DEBIT", amount, currency,
                from.getBalance(), reason, correlationId);
        writeOutboxEvent(to.getId(), transferId, creditEntryId, "CREDIT", amount, currency,
                to.getBalance(), reason, correlationId);

        return transfer;
    }

    private void writeOutboxEvent(UUID accountId, UUID transferId, UUID ledgerEntryId, String direction,
            BigDecimal amount, String currency, BigDecimal resultingBalance, String reason, String correlationId) {
        AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(UUID.randomUUID(), Instant.now(),
                correlationId, accountId, transferId, ledgerEntryId, direction, amount, currency,
                resultingBalance, reason);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
        OutboxEvent outboxEvent = new OutboxEvent(UUID.randomUUID(), "Account", accountId,
                "AccountBalanceChanged", Topics.ACCOUNT_BALANCE_CHANGED, payload);
        outboxRepository.save(outboxEvent);
    }
}
