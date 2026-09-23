package com.ledgerflow.settlement.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.events.SettlementStatusChangedEvent;
import com.ledgerflow.events.Topics;
import com.ledgerflow.settlement.domain.Settlement;
import com.ledgerflow.settlement.outbox.OutboxEvent;
import com.ledgerflow.settlement.outbox.OutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes a {@link SettlementStatusChangedEvent} outbox row in its own committed transaction
 * every time a settlement reaches a new status, for ledger-query-service and
 * notification-service to consume via Kafka. */
@Component
public class SettlementEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public SettlementEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishStatusChanged(Settlement settlement) {
        List<UUID> participants = settlement.getSteps().stream()
                .map(step -> step.getPayeeAccountId())
                .toList();
        SettlementStatusChangedEvent event = new SettlementStatusChangedEvent(UUID.randomUUID(), Instant.now(),
                settlement.getId().toString(), settlement.getId(), settlement.getStatus().name(),
                settlement.getFailureReason(), participants);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
        outboxRepository.save(new OutboxEvent(UUID.randomUUID(), "Settlement", settlement.getId(),
                "SettlementStatusChanged", Topics.SETTLEMENT_STATUS_CHANGED, payload));
    }
}
