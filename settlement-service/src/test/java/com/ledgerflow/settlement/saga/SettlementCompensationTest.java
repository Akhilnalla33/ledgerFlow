package com.ledgerflow.settlement.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ledgerflow.settlement.SettlementServiceApplication;
import com.ledgerflow.settlement.client.AccountServiceClient;
import com.ledgerflow.settlement.domain.Settlement;
import com.ledgerflow.settlement.domain.SettlementStatus;
import com.ledgerflow.settlement.domain.StepStatus;
import com.ledgerflow.settlement.support.TestSecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

/**
 * THE saga-compensation centerpiece test. A 4-way settlement (1 payer, 4 payees) is executed
 * with a mocked {@link AccountServiceClient} that succeeds on the first two legs and then
 * throws on the third, simulating account-service rejecting a transfer mid-saga (e.g.
 * insufficient funds, or a downstream outage). The orchestrator must not leave the system
 * half-applied: it has to issue compensating transfers for the two legs that already
 * succeeded, and the settlement/step rows must end up in a state that says exactly that —
 * COMPENSATED, with steps 1 and 2 COMPENSATED, step 3 FAILED, and step 4 never attempted.
 */
@SpringBootTest(classes = SettlementServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestSecurityConfig.class)
class SettlementCompensationTest {

    @Autowired
    private SettlementOrchestrator orchestrator;

    @MockBean
    private AccountServiceClient accountServiceClient;

    @Test
    void midSagaFailureCompensatesEveryAlreadyCompletedStep() {
        UUID payer = UUID.randomUUID();
        UUID payee1 = UUID.randomUUID();
        UUID payee2 = UUID.randomUUID();
        UUID payee3 = UUID.randomUUID();
        UUID payee4 = UUID.randomUUID();

        UUID transfer1 = UUID.randomUUID();
        UUID transfer2 = UUID.randomUUID();
        UUID compensation1 = UUID.randomUUID();
        UUID compensation2 = UUID.randomUUID();

        // Steps 0 and 1 (forward legs) succeed.
        when(accountServiceClient.transfer(anyString(), eq(payer), eq(payee1), any(), anyString(), any()))
                .thenReturn(new AccountServiceClient.TransferResult(transfer1));
        when(accountServiceClient.transfer(anyString(), eq(payer), eq(payee2), any(), anyString(), any()))
                .thenReturn(new AccountServiceClient.TransferResult(transfer2));
        // Step 2 (forward leg to payee3) fails.
        when(accountServiceClient.transfer(anyString(), eq(payer), eq(payee3), any(), anyString(), any()))
                .thenThrow(new AccountServiceClient.AccountServiceCallException("insufficient funds", null));
        // Compensations (payee -> payer) succeed.
        when(accountServiceClient.transfer(anyString(), eq(payee1), eq(payer), any(), anyString(), any()))
                .thenReturn(new AccountServiceClient.TransferResult(compensation1));
        when(accountServiceClient.transfer(anyString(), eq(payee2), eq(payer), any(), anyString(), any()))
                .thenReturn(new AccountServiceClient.TransferResult(compensation2));

        List<SettlementOrchestrator.PayeeShare> shares = List.of(
                new SettlementOrchestrator.PayeeShare(payee1, new BigDecimal("25.00")),
                new SettlementOrchestrator.PayeeShare(payee2, new BigDecimal("25.00")),
                new SettlementOrchestrator.PayeeShare(payee3, new BigDecimal("25.00")),
                new SettlementOrchestrator.PayeeShare(payee4, new BigDecimal("25.00")));

        Settlement created = orchestrator.createSettlement(payer, shares, "USD", "quarterly-split");
        Settlement result = orchestrator.execute(created.getId());

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.COMPENSATED);
        assertThat(result.getFailureReason()).contains("Step 2 failed");

        assertThat(result.getSteps()).hasSize(4);
        assertThat(result.getSteps().get(0).getStatus()).isEqualTo(StepStatus.COMPENSATED);
        assertThat(result.getSteps().get(0).getCompensationTransferId()).isEqualTo(compensation1);
        assertThat(result.getSteps().get(1).getStatus()).isEqualTo(StepStatus.COMPENSATED);
        assertThat(result.getSteps().get(1).getCompensationTransferId()).isEqualTo(compensation2);
        assertThat(result.getSteps().get(2).getStatus()).isEqualTo(StepStatus.FAILED);
        // Step 4 (payee4) was never reached: the saga fails fast and compensates rather than
        // continuing to apply further debits after a step has already failed.
        assertThat(result.getSteps().get(3).getStatus()).isEqualTo(StepStatus.PENDING);
        assertThat(result.getSteps().get(3).getTransferId()).isNull();
    }

    @Test
    void happyPathCompletesAllStepsWithNoCompensation() {
        UUID payer = UUID.randomUUID();
        UUID payee1 = UUID.randomUUID();
        UUID payee2 = UUID.randomUUID();

        when(accountServiceClient.transfer(anyString(), eq(payer), any(UUID.class), any(), anyString(), any()))
                .thenReturn(new AccountServiceClient.TransferResult(UUID.randomUUID()));

        List<SettlementOrchestrator.PayeeShare> shares = List.of(
                new SettlementOrchestrator.PayeeShare(payee1, new BigDecimal("10.00")),
                new SettlementOrchestrator.PayeeShare(payee2, new BigDecimal("10.00")));

        Settlement created = orchestrator.createSettlement(payer, shares, "USD", "lunch");
        Settlement result = orchestrator.execute(created.getId());

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(result.getSteps()).allSatisfy(step -> assertThat(step.getStatus()).isEqualTo(StepStatus.COMPLETED));
    }
}
