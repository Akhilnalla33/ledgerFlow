package com.ledgerflow.account.web;

import com.ledgerflow.account.dto.TransferRequest;
import com.ledgerflow.account.dto.TransferResponse;
import com.ledgerflow.account.service.TransferService;
import com.ledgerflow.account.web.error.MissingIdempotencyKeyException;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @ownership.ownsAccount(#request.fromAccountId(), authentication)")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new MissingIdempotencyKeyException();
        }
        String correlationId = MDC.get("correlationId");
        TransferResponse response = transferService.transfer(idempotencyKey, request, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
