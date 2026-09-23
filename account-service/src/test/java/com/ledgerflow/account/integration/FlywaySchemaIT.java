package com.ledgerflow.account.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerflow.account.AccountServiceApplication;
import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.dto.TransferRequest;
import com.ledgerflow.account.repository.AccountRepository;
import com.ledgerflow.account.repository.LedgerEntryRepository;
import com.ledgerflow.account.service.TransferService;
import com.ledgerflow.account.support.TestSecurityConfig;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the whole flow against a real PostgreSQL container with the real Flyway migration
 * (V1__init_schema.sql), not the ad-hoc Hibernate-generated H2 schema the rest of the test
 * suite uses for speed. This is the one place that actually proves:
 * <ul>
 *   <li>the migration SQL is valid Postgres and Flyway applies it cleanly;</li>
 *   <li>the {@code ledger_entries} append-only trigger genuinely rejects UPDATE/DELETE on
 *       real Postgres (H2 can't run the plpgsql trigger function at all);</li>
 *   <li>a transfer through the full Spring context posts correctly against a real database.</li>
 * </ul>
 *
 * <p>Gated behind the {@code docker-integration-tests} Maven profile
 * ({@code mvn verify -Pdocker-integration-tests}) since it requires a local Docker daemon —
 * see the root README's "Environment limitations" section for why this could not be executed
 * in the sandbox this project was built in, and why it is still included and wired into CI
 * (GitHub-hosted runners have Docker preinstalled).
 */
@Testcontainers
@SpringBootTest(classes = AccountServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestSecurityConfig.class)
class FlywaySchemaIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("account_db")
            .withUsername("ledgerflow")
            .withPassword("ledgerflow");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private TransferService transferService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void migrationAppliesAndATransferPostsCorrectlyAgainstRealPostgres() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Account from = tx.execute(status -> accountRepository.save(new Account(UUID.randomUUID(), "payer", "USD")));
        Account to = tx.execute(status -> accountRepository.save(new Account(UUID.randomUUID(), "payee", "USD")));
        Account funding = tx.execute(status -> {
            Account f = new Account(UUID.randomUUID(), "funding", "USD");
            f.credit(new BigDecimal("500.00"));
            return accountRepository.save(f);
        });

        transferService.transfer("it-seed-" + from.getId(),
                new TransferRequest(funding.getId(), from.getId(), new BigDecimal("200.00"), "USD", "seed"),
                "it-correlation");
        transferService.transfer("it-transfer-1",
                new TransferRequest(from.getId(), to.getId(), new BigDecimal("75.00"), "USD", "rent"),
                "it-correlation");

        Account reloadedFrom = accountRepository.findById(from.getId()).orElseThrow();
        Account reloadedTo = accountRepository.findById(to.getId()).orElseThrow();
        assertThat(reloadedFrom.getBalance()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(reloadedTo.getBalance()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(ledgerEntryRepository.sumSignedAmountAll()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @Test
    void appendOnlyTriggerRejectsDirectMutationOfLedgerEntries() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Account from = tx.execute(status -> accountRepository.save(new Account(UUID.randomUUID(), "payer2", "USD")));
        Account funding = tx.execute(status -> {
            Account f = new Account(UUID.randomUUID(), "funding2", "USD");
            f.credit(new BigDecimal("100.00"));
            return accountRepository.save(f);
        });
        transferService.transfer("it-seed-2-" + from.getId(),
                new TransferRequest(funding.getId(), from.getId(), new BigDecimal("50.00"), "USD", "seed"),
                "it-correlation");

        UUID anyLedgerEntryId = ledgerEntryRepository.findAll().stream().findFirst().orElseThrow().getId();

        // A raw UPDATE against the append-only ledger_entries table must be rejected by the
        // V1__init_schema.sql trigger, on real Postgres, regardless of application logic. This
        // cannot be verified against the H2-backed unit test suite because H2 does not run the
        // plpgsql trigger function at all.
        jakarta.persistence.EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            assertThatThrownBy(() -> {
                em.createNativeQuery("UPDATE ledger_entries SET amount = amount + 1 WHERE id = ?1")
                        .setParameter(1, anyLedgerEntryId)
                        .executeUpdate();
            }).isInstanceOf(RuntimeException.class).hasMessageContaining("append-only");
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
    }
}
