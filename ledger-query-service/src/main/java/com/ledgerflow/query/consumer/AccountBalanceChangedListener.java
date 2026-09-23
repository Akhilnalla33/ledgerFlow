package com.ledgerflow.query.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.events.AccountBalanceChangedEvent;
import com.ledgerflow.events.Topics;
import com.ledgerflow.query.service.AccountBalanceProjector;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AccountBalanceChangedListener {

    private final AccountBalanceProjector projector;
    private final ObjectMapper objectMapper;

    public AccountBalanceChangedListener(AccountBalanceProjector projector, ObjectMapper objectMapper) {
        this.projector = projector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.ACCOUNT_BALANCE_CHANGED, groupId = "ledger-query-service")
    public void onMessage(String payload) throws Exception {
        projector.apply(objectMapper.readValue(payload, AccountBalanceChangedEvent.class));
    }
}
