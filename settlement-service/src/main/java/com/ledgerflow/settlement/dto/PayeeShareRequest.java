package com.ledgerflow.settlement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PayeeShareRequest(@NotNull UUID payeeAccountId, @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
