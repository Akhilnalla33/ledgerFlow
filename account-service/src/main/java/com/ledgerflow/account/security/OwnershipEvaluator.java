package com.ledgerflow.account.security;

import com.ledgerflow.account.repository.AccountRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Backs the {@code @ownership.ownsAccount(...)} expression in {@code @PreAuthorize}
 * annotations. The JWT's subject claim is treated as the owner id: a non-admin caller may
 * only act on accounts whose {@code owner_id} equals their own subject, enforced here rather
 * than trusted from a request parameter.
 */
@Component("ownership")
public class OwnershipEvaluator {

    private final AccountRepository accountRepository;

    public OwnershipEvaluator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public boolean ownsAccount(UUID accountId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return accountRepository.findById(accountId)
                .map(account -> account.getOwnerId().equals(authentication.getName()))
                .orElse(false);
    }
}
