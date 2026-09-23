package com.ledgerflow.account.web;

import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.domain.LedgerEntry;
import com.ledgerflow.account.dto.AccountResponse;
import com.ledgerflow.account.dto.CreateAccountRequest;
import com.ledgerflow.account.dto.LedgerEntryResponse;
import com.ledgerflow.account.dto.ReconciliationResponse;
import com.ledgerflow.account.repository.LedgerEntryRepository;
import com.ledgerflow.account.service.AccountService;
import com.ledgerflow.account.service.ReconciliationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final ReconciliationService reconciliationService;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountController(AccountService accountService, ReconciliationService reconciliationService,
            LedgerEntryRepository ledgerEntryRepository) {
        this.accountService = accountService;
        this.reconciliationService = reconciliationService;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #request.ownerId() == authentication.name")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.ownerId(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.ownsAccount(#id, authentication)")
    public AccountResponse getAccount(@PathVariable UUID id, Authentication authentication) {
        return AccountResponse.from(accountService.getAccount(id));
    }

    @GetMapping("/{id}/ledger-entries")
    @PreAuthorize("hasRole('ADMIN') or @ownership.ownsAccount(#id, authentication)")
    public List<LedgerEntryResponse> getLedgerEntries(@PathVariable UUID id, Authentication authentication,
            Pageable pageable) {
        Page<LedgerEntry> page = ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(id, pageable);
        return page.map(LedgerEntryResponse::from).getContent();
    }

    @GetMapping("/{id}/reconcile")
    @PreAuthorize("hasRole('ADMIN') or @ownership.ownsAccount(#id, authentication)")
    public ReconciliationResponse reconcile(@PathVariable UUID id, Authentication authentication) {
        return reconciliationService.reconcileAccount(id);
    }
}
