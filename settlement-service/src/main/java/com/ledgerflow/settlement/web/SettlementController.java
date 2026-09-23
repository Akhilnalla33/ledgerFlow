package com.ledgerflow.settlement.web;

import com.ledgerflow.settlement.domain.Settlement;
import com.ledgerflow.settlement.dto.CreateSettlementRequest;
import com.ledgerflow.settlement.dto.SettlementResponse;
import com.ledgerflow.settlement.saga.SettlementOrchestrator;
import com.ledgerflow.settlement.saga.SettlementPersister;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {

    private final SettlementOrchestrator orchestrator;
    private final SettlementPersister persister;

    public SettlementController(SettlementOrchestrator orchestrator, SettlementPersister persister) {
        this.orchestrator = orchestrator;
        this.persister = persister;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SettlementResponse> createAndExecute(@Valid @RequestBody CreateSettlementRequest request) {
        List<SettlementOrchestrator.PayeeShare> shares = request.payees().stream()
                .map(p -> new SettlementOrchestrator.PayeeShare(p.payeeAccountId(), p.amount()))
                .toList();
        Settlement created = orchestrator.createSettlement(request.payerAccountId(), shares, request.currency(),
                request.reason());
        Settlement result = orchestrator.execute(created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SettlementResponse.from(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public SettlementResponse get(@PathVariable UUID id) {
        return SettlementResponse.from(persister.load(id));
    }
}
