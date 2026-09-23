package com.ledgerflow.query.web;

import com.ledgerflow.query.dto.BalanceSnapshotResponse;
import com.ledgerflow.query.dto.SettlementAuditResponse;
import com.ledgerflow.query.dto.StatementEntryResponse;
import com.ledgerflow.query.repository.BalanceSnapshotRepository;
import com.ledgerflow.query.repository.SettlementAuditRepository;
import com.ledgerflow.query.repository.StatementEntryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class QueryController {

    private final BalanceSnapshotRepository balanceSnapshotRepository;
    private final StatementEntryRepository statementEntryRepository;
    private final SettlementAuditRepository settlementAuditRepository;

    public QueryController(BalanceSnapshotRepository balanceSnapshotRepository,
            StatementEntryRepository statementEntryRepository, SettlementAuditRepository settlementAuditRepository) {
        this.balanceSnapshotRepository = balanceSnapshotRepository;
        this.statementEntryRepository = statementEntryRepository;
        this.settlementAuditRepository = settlementAuditRepository;
    }

    @GetMapping("/accounts/{accountId}/balance")
    @PreAuthorize("isAuthenticated()")
    public BalanceSnapshotResponse balance(@PathVariable UUID accountId) {
        return balanceSnapshotRepository.findById(accountId)
                .map(BalanceSnapshotResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No balance projection yet"));
    }

    @GetMapping("/accounts/{accountId}/statement")
    @PreAuthorize("isAuthenticated()")
    public List<StatementEntryResponse> statement(@PathVariable UUID accountId, Pageable pageable) {
        return statementEntryRepository.findByAccountIdOrderByOccurredAtDesc(accountId, pageable)
                .map(StatementEntryResponse::from)
                .getContent();
    }

    @GetMapping("/settlements/{settlementId}/audit-trail")
    @PreAuthorize("isAuthenticated()")
    public List<SettlementAuditResponse> auditTrail(@PathVariable UUID settlementId) {
        return settlementAuditRepository.findBySettlementIdOrderByOccurredAtAsc(settlementId).stream()
                .map(SettlementAuditResponse::from)
                .toList();
    }
}
