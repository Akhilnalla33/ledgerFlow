package com.ledgerflow.query.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.events.SettlementStatusChangedEvent;
import com.ledgerflow.events.Topics;
import com.ledgerflow.query.service.SettlementAuditProjector;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SettlementStatusChangedListener {

    private final SettlementAuditProjector projector;
    private final ObjectMapper objectMapper;

    public SettlementStatusChangedListener(SettlementAuditProjector projector, ObjectMapper objectMapper) {
        this.projector = projector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.SETTLEMENT_STATUS_CHANGED, groupId = "ledger-query-service")
    public void onMessage(String payload) throws Exception {
        projector.apply(objectMapper.readValue(payload, SettlementStatusChangedEvent.class));
    }
}
