package com.ledgerflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.events.AccountBalanceChangedEvent;
import com.ledgerflow.events.SettlementStatusChangedEvent;
import com.ledgerflow.events.Topics;
import com.ledgerflow.notification.service.EventNotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private final EventNotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(EventNotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.ACCOUNT_BALANCE_CHANGED, groupId = "notification-service")
    public void onBalanceChanged(String payload) throws Exception {
        notificationService.onBalanceChanged(objectMapper.readValue(payload, AccountBalanceChangedEvent.class));
    }

    @KafkaListener(topics = Topics.SETTLEMENT_STATUS_CHANGED, groupId = "notification-service")
    public void onSettlementStatusChanged(String payload) throws Exception {
        notificationService.onSettlementStatusChanged(
                objectMapper.readValue(payload, SettlementStatusChangedEvent.class));
    }
}
