package com.ledgerflow.settlement.outbox;

import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAX_RETRIES_BEFORE_DEAD_LETTER = 10;
    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${ledgerflow.outbox.poll-interval-ms:500}")
    @Transactional
    @Retry(name = "outboxPublisher")
    public void publishPendingEvents() {
        List<OutboxEvent> batch = outboxRepository.findPendingBatchForUpdate(PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : batch) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload()).get();
            event.markPublished();
        } catch (Exception ex) {
            event.markRetry();
            log.warn("Failed to publish outbox event {} (attempt {}): {}",
                    event.getId(), event.getRetryCount(), ex.getMessage());
            if (event.getRetryCount() >= MAX_RETRIES_BEFORE_DEAD_LETTER) {
                event.markFailed();
                log.error("Outbox event {} exceeded max retries, marking FAILED for manual replay", event.getId());
            }
        }
    }
}
