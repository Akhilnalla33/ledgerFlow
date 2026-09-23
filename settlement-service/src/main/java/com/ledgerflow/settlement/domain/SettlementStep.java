package com.ledgerflow.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One payer -&gt; payee leg of a {@link Settlement}. The forward saga executes steps in
 * {@link #stepOrder}; on failure, {@link com.ledgerflow.settlement.saga.SettlementOrchestrator}
 * compensates every step already {@link StepStatus#COMPLETED}, in reverse order, by issuing an
 * equal-and-opposite transfer back to the payer.
 */
@Entity
@Table(name = "settlement_steps")
public class SettlementStep {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "payee_account_id", nullable = false)
    private UUID payeeAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "compensation_transfer_id")
    private UUID compensationTransferId;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SettlementStep() {
    }

    public SettlementStep(UUID id, int stepOrder, UUID payeeAccountId, BigDecimal amount) {
        this.id = id;
        this.stepOrder = stepOrder;
        this.payeeAccountId = payeeAccountId;
        this.amount = amount;
    }

    void attachTo(Settlement settlement) {
        this.settlement = settlement;
    }

    public void markCompleted(UUID transferId) {
        this.status = StepStatus.COMPLETED;
        this.transferId = transferId;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = StepStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markCompensated(UUID compensationTransferId) {
        this.status = StepStatus.COMPENSATED;
        this.compensationTransferId = compensationTransferId;
        this.updatedAt = Instant.now();
    }

    public Settlement getSettlement() {
        return settlement;
    }

    public UUID getId() {
        return id;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public UUID getPayeeAccountId() {
        return payeeAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public StepStatus getStatus() {
        return status;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getCompensationTransferId() {
        return compensationTransferId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
