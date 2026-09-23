package com.ledgerflow.account.web.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String correlationId,
        List<String> details) {
}
