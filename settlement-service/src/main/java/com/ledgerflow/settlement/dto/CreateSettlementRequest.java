package com.ledgerflow.settlement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

public record CreateSettlementRequest(
        @NotNull UUID payerAccountId,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        String reason,
        @NotEmpty @Valid List<PayeeShareRequest> payees) {
}
