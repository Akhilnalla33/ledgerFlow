package com.ledgerflow.settlement.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ledgerflow.settlement.SettlementServiceApplication;
import com.ledgerflow.settlement.client.AccountServiceClient;
import com.ledgerflow.settlement.domain.Settlement;
import com.ledgerflow.settlement.domain.SettlementStatus;
import com.ledgerflow.settlement.saga.SettlementOrchestrator;
import com.ledgerflow.settlement.support.TestSecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Same purpose as account-service's {@code FlywaySchemaIT}: proves the real Flyway migration
 * (V1__init_schema.sql, including the {@code settlements}/{@code settlement_steps} foreign key
 * and cascade behavior) works against real PostgreSQL, not just the Hibernate-generated H2
 * schema the rest of the suite uses. Gated behind {@code -Pdocker-integration-tests}; see the
 * root README's "Environment limitations" section.
 */
@Testcontainers
@SpringBootTest(classes = SettlementServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestSecurityConfig.class)
class SettlementFlywayIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("settlement_db")
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
    private SettlementOrchestrator orchestrator;

    @MockBean
    private AccountServiceClient accountServiceClient;

    @Test
    void settlementAndItsStepsPersistCorrectlyAgainstRealPostgres() {
        when(accountServiceClient.transfer(anyString(), any(), any(), any(), anyString(), any()))
                .thenReturn(new AccountServiceClient.TransferResult(UUID.randomUUID()));

        UUID payer = UUID.randomUUID();
        List<SettlementOrchestrator.PayeeShare> shares = List.of(
                new SettlementOrchestrator.PayeeShare(UUID.randomUUID(), new BigDecimal("15.00")),
                new SettlementOrchestrator.PayeeShare(UUID.randomUUID(), new BigDecimal("15.00")));

        Settlement created = orchestrator.createSettlement(payer, shares, "USD", "it-test");
        Settlement result = orchestrator.execute(created.getId());

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(result.getSteps()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
    }
}
