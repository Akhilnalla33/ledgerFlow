package com.ledgerflow.account.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerflow.account.AccountServiceApplication;
import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.dto.TransferRequest;
import com.ledgerflow.account.repository.AccountRepository;
import com.ledgerflow.account.repository.LedgerEntryRepository;
import com.ledgerflow.account.service.TransferService;
import com.ledgerflow.account.support.TestSecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * THE centerpiece test of this repository.
 *
 * <p>It fires many concurrent transfers, from many different threads, against a shared pool
 * of accounts — the exact scenario that produces a "lost update" if balance mutations aren't
 * concurrency-safe: thread A reads balance=100, thread B reads balance=100, A writes 90, B
 * writes 80, and A's debit vanishes. {@link Account#getVersion()} (a JPA {@code @Version}
 * column) is what prevents that here: every UPDATE includes {@code WHERE version = ?}, so the
 * second writer's UPDATE affects zero rows, Hibernate throws
 * {@link org.springframework.orm.ObjectOptimisticLockingFailureException}, and
 * {@link TransferService} retries that attempt against the now-current balance. See
 * ADR-0002 for why optimistic locking was chosen over {@code SELECT ... FOR UPDATE}.
 *
 * <p>The test's assertion is exact-integer arithmetic, not a tolerance: after N concurrent
 * transfers of a fixed amount out of one account (mixed with transfers back in), the final
 * balance must equal starting balance plus the exact net signed sum of every transfer — to
 * the cent. Any lost update shows up as a wrong final balance, deterministically, every run.
 */
@SpringBootTest(classes = AccountServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestSecurityConfig.class)
class ConcurrentTransferCorrectnessTest {

    @org.springframework.beans.factory.annotation.Autowired
    private TransferService transferService;
    @org.springframework.beans.factory.annotation.Autowired
    private AccountRepository accountRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @RepeatedTest(3)
    void manyConcurrentTransfersProduceExactFinalBalanceWithNoLostUpdates() throws InterruptedException {
        // Distinguishes idempotency keys across repetitions: this @SpringBootTest context
        // (and its in-memory H2 schema) is reused across repetitions and other test classes,
        // so keys must not collide with a previous run's.
        String runId = UUID.randomUUID().toString();
        Account hub = createAccount("hub-owner", "USD");
        List<Account> spokes = List.of(
                createAccount("spoke-1", "USD"),
                createAccount("spoke-2", "USD"),
                createAccount("spoke-3", "USD"),
                createAccount("spoke-4", "USD"));
        seedBalance(hub.getId(), new BigDecimal("100000.00"));
        for (Account spoke : spokes) {
            seedBalance(spoke.getId(), new BigDecimal("5000.00"));
        }

        int threadCount = 32;
        int transfersPerThread = 25;
        BigDecimal amountOut = new BigDecimal("10.00"); // hub -> spoke
        BigDecimal amountIn = new BigDecimal("4.00");   // spoke -> hub

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger outTransfers = new AtomicInteger();
        AtomicInteger inTransfers = new AtomicInteger();

        for (int t = 0; t < threadCount; t++) {
            int threadIndex = t;
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < transfersPerThread; i++) {
                        Account spoke = spokes.get((threadIndex + i) % spokes.size());
                        boolean outbound = (threadIndex + i) % 2 == 0;
                        TransferRequest request = outbound
                                ? new TransferRequest(hub.getId(), spoke.getId(), amountOut, "USD", "load-out")
                                : new TransferRequest(spoke.getId(), hub.getId(), amountIn, "USD", "load-in");
                        transferService.transfer("conc-" + runId + "-" + threadIndex + "-" + i, request,
                                "test-correlation");
                        if (outbound) {
                            outTransfers.incrementAndGet();
                        } else {
                            inTransfers.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = doneLatch.await(60, TimeUnit.SECONDS);
        pool.shutdown();
        assertThat(finished).as("all transfer threads completed within timeout").isTrue();

        BigDecimal expectedHubBalance = new BigDecimal("100000.00")
                .subtract(amountOut.multiply(BigDecimal.valueOf(outTransfers.get())))
                .add(amountIn.multiply(BigDecimal.valueOf(inTransfers.get())));

        Account reloadedHub = accountRepository.findById(hub.getId()).orElseThrow();
        assertThat(reloadedHub.getBalance())
                .as("hub balance after %d concurrent threads x %d transfers each (no lost updates)",
                        threadCount, transfersPerThread)
                .isEqualByComparingTo(expectedHubBalance);

        // Cross-check: the balance is also exactly what the immutable ledger entries say it
        // is, independent of the cached Account.balance column.
        BigDecimal ledgerDerivedBalance = ledgerEntryRepository.sumSignedAmountByAccountId(hub.getId());
        assertThat(ledgerDerivedBalance).isEqualByComparingTo(reloadedHub.getBalance());

        // System-wide double-entry invariant: money was moved, never created or destroyed.
        assertThat(ledgerEntryRepository.sumSignedAmountAll())
                .as("sum of every signed ledger entry in the system must be exactly zero")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Account createAccount(String ownerId, String currency) {
        return accountRepository.save(new Account(UUID.randomUUID(), ownerId, currency));
    }

    private UUID fundingAccountId;

    private void seedBalance(UUID accountId, BigDecimal amount) {
        // Seed via a real transfer from a funding account so every account's balance stays
        // fully explained by ledger entries, rather than poking the balance column directly.
        if (fundingAccountId == null) {
            Account funding = accountRepository.save(new Account(UUID.randomUUID(), "system-funding", "USD"));
            fundingAccountId = funding.getId();
            // The one and only direct balance write in this whole test suite: it originates
            // test money the same way a real deposit/mint entrypoint would, outside the scope
            // of what this test measures. Every subsequent movement goes through
            // TransferService and is fully ledgered.
            funding.credit(new BigDecimal("10000000.00"));
            accountRepository.save(funding);
        }
        transferService.transfer("seed-" + accountId, new TransferRequest(fundingAccountId, accountId, amount,
                "USD", "seed"), "seed-correlation");
    }
}
