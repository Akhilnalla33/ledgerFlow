package com.ledgerflow.account.service;

import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.dto.ReconciliationResponse;
import com.ledgerflow.account.repository.AccountRepository;
import com.ledgerflow.account.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the ledger is trustworthy rather than assuming it. Two checks:
 * <ul>
 *   <li>per-account: the account's cached {@code balance} column must equal the signed sum of
 *       every {@code ledger_entries} row for that account;</li>
 *   <li>system-wide: the signed sum of every ledger entry in the whole database must be
 *       exactly zero, because every transfer posts one DEBIT and one CREDIT of equal amount —
 *       money is never created or destroyed, only moved.</li>
 * </ul>
 * A non-zero global sum or a per-account mismatch means either a bug slipped past the
 * append-only trigger and the optimistic-locking guard, or the database was tampered with
 * directly — either way this is the canary that would catch it.
 */
@Service
public class ReconciliationService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public ReconciliationService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse reconcileAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        BigDecimal derived = ledgerEntryRepository.sumSignedAmountByAccountId(accountId);
        boolean balanced = account.getBalance().compareTo(derived) == 0;
        return new ReconciliationResponse(accountId, account.getBalance(), derived, balanced);
    }

    /** @return true iff the sum of every signed ledger entry in the system is exactly zero. */
    @Transactional(readOnly = true)
    public boolean isLedgerGloballyBalanced() {
        return ledgerEntryRepository.sumSignedAmountAll().compareTo(BigDecimal.ZERO) == 0;
    }

    @Transactional(readOnly = true)
    public List<UUID> findUnbalancedAccounts() {
        return accountRepository.findAll().stream()
                .filter(a -> a.getBalance().compareTo(ledgerEntryRepository.sumSignedAmountByAccountId(a.getId())) != 0)
                .map(Account::getId)
                .toList();
    }
}
