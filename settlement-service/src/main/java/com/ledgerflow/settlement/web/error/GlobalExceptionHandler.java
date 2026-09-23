package com.ledgerflow.settlement.web.error;

import com.ledgerflow.settlement.client.AccountServiceClient;
import com.ledgerflow.settlement.saga.SettlementNotFoundException;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(SettlementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(SettlementNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(AccountServiceClient.AccountServiceCallException.class)
    public ResponseEntity<ErrorResponse> handleAccountServiceFailure(AccountServiceClient.AccountServiceCallException ex) {
        return build(HttpStatus.BAD_GATEWAY, "Settlement failed and was compensated: " + ex.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, java.util.List<String> details) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message,
                MDC.get("correlationId"), details);
        return ResponseEntity.status(status).body(body);
    }
}
