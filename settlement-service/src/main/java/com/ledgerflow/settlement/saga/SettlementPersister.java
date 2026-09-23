package com.ledgerflow.settlement.saga;

import com.ledgerflow.settlement.domain.Settlement;
import com.ledgerflow.settlement.domain.SettlementStatus;
import com.ledgerflow.settlement.domain.SettlementStep;
import com.ledgerflow.settlement.repository.SettlementRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every state transition below commits in its own {@code REQUIRES_NEW} transaction, separate
 * from the in-flight synchronous call to account-service that {@link SettlementOrchestrator}
 * makes between them. That way each step's outcome is durable the instant it happens: if the
 * orchestrator process crashed between two steps, the database would show exactly how far the
 * saga got, rather than losing progress that only lived in memory.
 */
@Component
public class SettlementPersister {

    private final SettlementRepository settlementRepository;

    public SettlementPersister(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    @Transactional(readOnly = true)
    public Settlement load(UUID settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementNotFoundException(settlementId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Settlement create(Settlement settlement) {
        return settlementRepository.save(settlement);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transition(UUID settlementId, SettlementStatus status) {
        Settlement settlement = settlementRepository.findById(settlementId).orElseThrow();
        settlement.transitionTo(status);
        settlementRepository.save(settlement);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID settlementId, String reason) {
        Settlement settlement = settlementRepository.findById(settlementId).orElseThrow();
        settlement.fail(reason);
        settlementRepository.save(settlement);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void stepCompleted(UUID settlementId, UUID stepId, UUID transferId) {
        SettlementStep step = findStep(settlementId, stepId);
        step.markCompleted(transferId);
        settlementRepository.save(step.getSettlement());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void stepFailed(UUID settlementId, UUID stepId, String reason) {
        SettlementStep step = findStep(settlementId, stepId);
        step.markFailed(reason);
        settlementRepository.save(step.getSettlement());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void stepCompensated(UUID settlementId, UUID stepId, UUID compensationTransferId) {
        SettlementStep step = findStep(settlementId, stepId);
        step.markCompensated(compensationTransferId);
        settlementRepository.save(step.getSettlement());
    }

    private SettlementStep findStep(UUID settlementId, UUID stepId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementNotFoundException(settlementId));
        return settlement.getSteps().stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Step not found: " + stepId));
    }
}
