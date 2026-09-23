package com.ledgerflow.settlement.saga;

import com.ledgerflow.settlement.client.AccountServiceClient;
import com.ledgerflow.settlement.domain.Settlement;
import com.ledgerflow.settlement.domain.SettlementStatus;
import com.ledgerflow.settlement.domain.SettlementStep;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Explicit saga orchestrator (see ADR-0001 for orchestration-vs-choreography) driving a
 * settlement through: STARTED -&gt; IN_PROGRESS -&gt; (each step debits the payer and credits
 * one payee, via a synchronous call to account-service) -&gt; COMPLETED. If any step fails, the
 * orchestrator does not leave the payer partially debited: it walks every already-COMPLETED
 * step, in reverse order, and issues an equal-and-opposite compensating transfer back from
 * that payee to the payer, then marks the settlement COMPENSATED.
 *
 * <p>Every step's outcome — completed, failed, or compensated — is durably persisted (via
 * {@link SettlementPersister}, each call its own committed transaction) the instant it
 * happens, and every call out to account-service carries a stable
 * {@code <settlementId>-<stepOrder>[-compensate]} idempotency key, so this method itself can
 * be safely retried end-to-end without double-applying any leg.
 */
@Service
public class SettlementOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SettlementOrchestrator.class);

    private final SettlementPersister persister;
    private final AccountServiceClient accountServiceClient;
    private final SettlementEventPublisher eventPublisher;

    public SettlementOrchestrator(SettlementPersister persister, AccountServiceClient accountServiceClient,
            SettlementEventPublisher eventPublisher) {
        this.persister = persister;
        this.accountServiceClient = accountServiceClient;
        this.eventPublisher = eventPublisher;
    }

    public Settlement createSettlement(UUID payerAccountId, List<PayeeShare> shares, String currency, String reason) {
        BigDecimal total = shares.stream().map(PayeeShare::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Settlement settlement = new Settlement(UUID.randomUUID(), payerAccountId, currency, total, reason);
        int order = 0;
        for (PayeeShare share : shares) {
            settlement.addStep(new SettlementStep(UUID.randomUUID(), order++, share.payeeAccountId(), share.amount()));
        }
        return persister.create(settlement);
    }

    public Settlement execute(UUID settlementId) {
        Settlement settlement = persister.load(settlementId);
        persister.transition(settlementId, SettlementStatus.IN_PROGRESS);

        List<SettlementStep> completedSteps = new ArrayList<>();
        for (SettlementStep step : settlement.getSteps()) {
            try {
                AccountServiceClient.TransferResult result = accountServiceClient.transfer(
                        forwardKey(settlement, step), settlement.getPayerAccountId(), step.getPayeeAccountId(),
                        step.getAmount(), settlement.getCurrency(), settlement.getReason());
                persister.stepCompleted(settlementId, step.getId(), result.transferId());
                completedSteps.add(step);
            } catch (RuntimeException ex) {
                log.warn("Settlement {} step {} failed, starting compensation: {}", settlementId,
                        step.getStepOrder(), ex.getMessage());
                persister.stepFailed(settlementId, step.getId(), ex.getMessage());
                compensate(settlement, completedSteps);
                persister.fail(settlementId, "Step " + step.getStepOrder() + " failed: " + ex.getMessage());
                persister.transition(settlementId, SettlementStatus.COMPENSATED);
                Settlement finalState = persister.load(settlementId);
                eventPublisher.publishStatusChanged(finalState);
                return finalState;
            }
        }

        persister.transition(settlementId, SettlementStatus.COMPLETED);
        Settlement finalState = persister.load(settlementId);
        eventPublisher.publishStatusChanged(finalState);
        return finalState;
    }

    private void compensate(Settlement settlement, List<SettlementStep> completedSteps) {
        persister.transition(settlement.getId(), SettlementStatus.COMPENSATING);
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SettlementStep step = completedSteps.get(i);
            AccountServiceClient.TransferResult result = accountServiceClient.transfer(
                    compensationKey(settlement, step), step.getPayeeAccountId(), settlement.getPayerAccountId(),
                    step.getAmount(), settlement.getCurrency(), "compensation for settlement " + settlement.getId());
            persister.stepCompensated(settlement.getId(), step.getId(), result.transferId());
        }
    }

    private String forwardKey(Settlement settlement, SettlementStep step) {
        return settlement.getId() + "-" + step.getStepOrder();
    }

    private String compensationKey(Settlement settlement, SettlementStep step) {
        return settlement.getId() + "-" + step.getStepOrder() + "-compensate";
    }

    public record PayeeShare(UUID payeeAccountId, BigDecimal amount) {
    }
}
