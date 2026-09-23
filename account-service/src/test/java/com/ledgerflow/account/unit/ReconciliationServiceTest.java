package com.ledgerflow.account.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.dto.ReconciliationResponse;
import com.ledgerflow.account.repository.AccountRepository;
import com.ledgerflow.account.repository.LedgerEntryRepository;
import com.ledgerflow.account.service.ReconciliationService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void flagsAccountAsBalancedWhenCachedBalanceMatchesLedgerSum() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, "owner", "USD");
        account.credit(new BigDecimal("50.00"));
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(account));
        when(ledgerEntryRepository.sumSignedAmountByAccountId(accountId)).thenReturn(new BigDecimal("50.00"));

        ReconciliationService service = new ReconciliationService(accountRepository, ledgerEntryRepository);
        ReconciliationResponse response = service.reconcileAccount(accountId);

        assertThat(response.balanced()).isTrue();
    }

    @Test
    void flagsAccountAsUnbalancedWhenCachedBalanceDriftsFromLedgerSum() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, "owner", "USD");
        account.credit(new BigDecimal("50.00"));
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(account));
        // Simulates corruption: the ledger only justifies 40.00, not the cached 50.00.
        when(ledgerEntryRepository.sumSignedAmountByAccountId(accountId)).thenReturn(new BigDecimal("40.00"));

        ReconciliationService service = new ReconciliationService(accountRepository, ledgerEntryRepository);
        ReconciliationResponse response = service.reconcileAccount(accountId);

        assertThat(response.balanced()).isFalse();
    }

    @Test
    void globalLedgerIsBalancedOnlyWhenSignedSumIsExactlyZero() {
        when(ledgerEntryRepository.sumSignedAmountAll()).thenReturn(BigDecimal.ZERO);
        ReconciliationService service = new ReconciliationService(accountRepository, ledgerEntryRepository);
        assertThat(service.isLedgerGloballyBalanced()).isTrue();
    }
}
