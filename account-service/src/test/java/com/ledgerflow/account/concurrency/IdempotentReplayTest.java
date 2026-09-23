package com.ledgerflow.account.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerflow.account.AccountServiceApplication;
import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.dto.TransferRequest;
import com.ledgerflow.account.dto.TransferResponse;
import com.ledgerflow.account.repository.AccountRepository;
import com.ledgerflow.account.repository.LedgerEntryRepository;
import com.ledgerflow.account.repository.TransferRepository;
import com.ledgerflow.account.service.TransferService;
import com.ledgerflow.account.support.TestSecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Proves the {@code Idempotency-Key} contract: firing the *same* transfer request concurrently
 * N times, with the same key, applies it exactly once. Every caller gets back an identical
 * {@link TransferResponse} (same {@code transferId}), but only one {@code Transfer} row and
 * one pair of ledger entries actually exist afterward, and the account balance only moved by
 * the amount once — not N times.
 */
@SpringBootTest(classes = AccountServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestSecurityConfig.class)
class IdempotentReplayTest {

    @Autowired
    private TransferService transferService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private TransferRepository transferRepository;

    @Test
    void sameIdempotencyKeyFiredConcurrentlyAppliesExactlyOnce() throws InterruptedException {
        Account from = accountRepository.save(new Account(UUID.randomUUID(), "payer", "USD"));
        Account to = accountRepository.save(new Account(UUID.randomUUID(), "payee", "USD"));
        seed(from.getId(), new BigDecimal("500.00"));

        TransferRequest request = new TransferRequest(from.getId(), to.getId(), new BigDecimal("25.00"), "USD", "rent");
        String idempotencyKey = "idem-" + UUID.randomUUID();

        int concurrentCallers = 20;
        ExecutorService pool = Executors.newFixedThreadPool(concurrentCallers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentCallers);
        List<TransferResponse> responses = new CopyOnWriteArrayList<>();

        for (int i = 0; i < concurrentCallers; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    responses.add(transferService.transfer(idempotencyKey, request, "test-correlation"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startGate.countDown();
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(responses).hasSize(concurrentCallers);
        Set<UUID> distinctTransferIds = responses.stream().map(TransferResponse::transferId)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(distinctTransferIds)
                .as("every concurrent caller must receive the SAME transferId")
                .hasSize(1);

        UUID transferId = distinctTransferIds.iterator().next();
        assertThat(transferRepository.findById(transferId)).isPresent();
        assertThat(ledgerEntryRepository.findByTransferId(transferId))
                .as("exactly one balanced DEBIT/CREDIT pair must exist for this transfer, not one pair per caller")
                .hasSize(2);

        Account reloadedFrom = accountRepository.findById(from.getId()).orElseThrow();
        assertThat(reloadedFrom.getBalance())
                .as("balance must have moved by 25.00 exactly once, not %d times", concurrentCallers)
                .isEqualByComparingTo(new BigDecimal("475.00"));
    }

    private void seed(UUID accountId, BigDecimal amount) {
        Account funding = accountRepository.save(new Account(UUID.randomUUID(), "system-funding", "USD"));
        funding.credit(amount);
        accountRepository.save(funding);
        transferService.transfer("seed-" + accountId, new TransferRequest(funding.getId(), accountId, amount,
                "USD", "seed"), "seed-correlation");
    }
}
