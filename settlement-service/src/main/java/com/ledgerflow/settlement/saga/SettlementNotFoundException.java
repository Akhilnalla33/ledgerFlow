package com.ledgerflow.settlement.saga;

import java.util.UUID;

public class SettlementNotFoundException extends RuntimeException {

    public SettlementNotFoundException(UUID settlementId) {
        super("Settlement not found: " + settlementId);
    }
}
