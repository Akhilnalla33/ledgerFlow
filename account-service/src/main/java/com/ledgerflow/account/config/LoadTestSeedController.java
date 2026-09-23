package com.ledgerflow.account.config;

import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Only registered under the {@code loadtest} profile. Mints money directly into a funding
 * account's cached balance, bypassing the double-entry ledger entirely — that is exactly what
 * a real "external money enters the system" boundary (a bank deposit feed, in production) would
 * do, and it deliberately does NOT exist in the account-service API surface used anywhere else
 * in this repo. Its only purpose is giving {@code scripts/loadtest/run-load-test.sh} a funded
 * account to run transfers from, without needing a real money-in integration for a load test.
 */
@RestController
@Profile("loadtest")
public class LoadTestSeedController {

    private final AccountRepository accountRepository;

    public LoadTestSeedController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @PostMapping("/loadtest-only/mint")
    @Transactional
    public Map<String, Object> mint(@RequestParam UUID accountId, @RequestParam BigDecimal amount) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.credit(amount);
        accountRepository.save(account);
        return Map.of("accountId", accountId, "balance", account.getBalance());
    }
}
