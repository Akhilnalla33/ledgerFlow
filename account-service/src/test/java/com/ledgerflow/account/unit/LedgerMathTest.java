package com.ledgerflow.account.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.domain.EntryType;
import com.ledgerflow.account.domain.InsufficientFundsException;
import com.ledgerflow.account.domain.LedgerEntry;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LedgerMathTest {

    @Test
    void debitAndCreditOfSameAmountNetToZero() {
        LedgerEntry debit = new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EntryType.DEBIT, new BigDecimal("42.50"), "USD", BigDecimal.ZERO, "test");
        LedgerEntry credit = new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EntryType.CREDIT, new BigDecimal("42.50"), "USD", BigDecimal.ZERO, "test");

        assertThat(debit.signedAmount().add(credit.signedAmount())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void accountDebitReducesBalance() {
        Account account = new Account(UUID.randomUUID(), "owner", "USD");
        account.credit(new BigDecimal("100.00"));
        account.debit(new BigDecimal("30.00"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void accountCannotBeDebitedBelowZero() {
        Account account = new Account(UUID.randomUUID(), "owner", "USD");
        account.credit(new BigDecimal("10.00"));

        assertThatThrownBy(() -> account.debit(new BigDecimal("10.01")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void zeroOrNegativeAmountsAreRejected() {
        Account account = new Account(UUID.randomUUID(), "owner", "USD");
        assertThatThrownBy(() -> account.credit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.debit(new BigDecimal("-5.00"))).isInstanceOf(IllegalArgumentException.class);
    }
}
