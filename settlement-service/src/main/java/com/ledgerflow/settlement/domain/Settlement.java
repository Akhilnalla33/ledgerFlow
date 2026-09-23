package com.ledgerflow.settlement.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The saga aggregate: one payer splitting {@link #totalAmount} across N
 * {@link SettlementStep} payees. {@link SettlementOrchestrator} drives this through the state
 * machine STARTED -&gt; IN_PROGRESS -&gt; COMPLETED on the happy path, or STARTED -&gt;
 * IN_PROGRESS -&gt; COMPENSATING -&gt; COMPENSATED if any step fails partway through — see
 * ADR-0001 for why this is orchestrated rather than choreographed.
 */
@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    private UUID id;

    @Column(name = "payer_account_id", nullable = false)
    private UUID payerAccountId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status = SettlementStatus.STARTED;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(length = 256)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("stepOrder asc")
    private List<SettlementStep> steps = new ArrayList<>();

    protected Settlement() {
    }

    public Settlement(UUID id, UUID payerAccountId, String currency, BigDecimal totalAmount, String reason) {
        this.id = id;
        this.payerAccountId = payerAccountId;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.reason = reason;
    }

    public void addStep(SettlementStep step) {
        step.attachTo(this);
        steps.add(step);
    }

    public void transitionTo(SettlementStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPayerAccountId() {
        return payerAccountId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<SettlementStep> getSteps() {
        return steps;
    }
}
