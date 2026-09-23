package com.ledgerflow.settlement.dto;

import com.ledgerflow.settlement.domain.Settlement;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SettlementResponse(
        UUID id, UUID payerAccountId, String currency, BigDecimal totalAmount, String status,
        String failureReason, List<StepResponse> steps) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(settlement.getId(), settlement.getPayerAccountId(), settlement.getCurrency(),
                settlement.getTotalAmount(), settlement.getStatus().name(), settlement.getFailureReason(),
                settlement.getSteps().stream().map(StepResponse::from).toList());
    }

    public record StepResponse(int stepOrder, UUID payeeAccountId, BigDecimal amount, String status,
            UUID transferId, UUID compensationTransferId) {

        public static StepResponse from(com.ledgerflow.settlement.domain.SettlementStep step) {
            return new StepResponse(step.getStepOrder(), step.getPayeeAccountId(), step.getAmount(),
                    step.getStatus().name(), step.getTransferId(), step.getCompensationTransferId());
        }
    }
}
