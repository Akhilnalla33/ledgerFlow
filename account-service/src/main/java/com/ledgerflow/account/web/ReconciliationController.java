package com.ledgerflow.account.web;

import com.ledgerflow.account.service.ReconciliationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * System-wide reconciliation: proves the double-entry invariant holds across every account,
 * not just one at a time. {@code ADMIN}-only since it scans the whole ledger.
 */
@RestController
@RequestMapping("/api/v1/ledger")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/reconciliation")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> reconcileAll() {
        boolean globallyBalanced = reconciliationService.isLedgerGloballyBalanced();
        List<UUID> unbalancedAccounts = reconciliationService.findUnbalancedAccounts();
        return Map.of(
                "globallyBalanced", globallyBalanced,
                "unbalancedAccountCount", unbalancedAccounts.size(),
                "unbalancedAccountIds", unbalancedAccounts);
    }
}
