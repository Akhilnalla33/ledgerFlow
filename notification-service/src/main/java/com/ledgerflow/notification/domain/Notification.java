package com.ledgerflow.notification.domain;

public record Notification(String recipientHint, String subject, String body) {
}
