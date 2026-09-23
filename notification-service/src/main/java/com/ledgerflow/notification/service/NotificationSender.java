package com.ledgerflow.notification.service;

import com.ledgerflow.notification.domain.Notification;

/**
 * Deliberately not wired to a real email/webhook provider (out of scope per the project
 * brief) — {@link LoggingNotificationSender} is the only implementation, and it just logs.
 * The abstraction exists so a real provider (SES, Twilio, a webhook POST) could be dropped in
 * behind this interface without touching the Kafka consumers that produce notifications.
 */
public interface NotificationSender {

    void send(Notification notification);
}
