package com.ledgerflow.notification.service;

import com.ledgerflow.events.AccountBalanceChangedEvent;
import com.ledgerflow.events.SettlementStatusChangedEvent;
import com.ledgerflow.notification.domain.Notification;
import org.springframework.stereotype.Service;

@Service
public class EventNotificationService {

    private final NotificationSender sender;

    public EventNotificationService(NotificationSender sender) {
        this.sender = sender;
    }

    public void onBalanceChanged(AccountBalanceChangedEvent event) {
        String subject = event.getDirection() + " of " + event.getAmount() + " " + event.getCurrency();
        String body = "Account " + event.getAccountId() + " balance is now " + event.getResultingBalance()
                + " " + event.getCurrency() + " (transfer " + event.getTransferId() + ")";
        sender.send(new Notification(event.getAccountId().toString(), subject, body));
    }

    public void onSettlementStatusChanged(SettlementStatusChangedEvent event) {
        String subject = "Settlement " + event.getSettlementId() + " is now " + event.getStatus();
        String body = event.getFailureReason() != null
                ? "Failure reason: " + event.getFailureReason()
                : "Participants: " + event.getParticipantAccountIds();
        sender.send(new Notification(event.getSettlementId().toString(), subject, body));
    }
}
