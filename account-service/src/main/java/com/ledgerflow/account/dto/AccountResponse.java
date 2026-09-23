package com.ledgerflow.account.dto;

import com.ledgerflow.account.domain.Account;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, String ownerId, String currency, BigDecimal balance, String status) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getOwnerId(), account.getCurrency(),
                account.getBalance(), account.getStatus());
    }
}
