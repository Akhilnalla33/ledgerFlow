package com.ledgerflow.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReconciliationResponse(
        UUID accountId, BigDecimal cachedBalance, BigDecimal ledgerDerivedBalance, boolean balanced) {
}
