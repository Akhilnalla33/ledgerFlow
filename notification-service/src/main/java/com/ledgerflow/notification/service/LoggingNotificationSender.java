package com.ledgerflow.notification.service;

import com.ledgerflow.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(Notification notification) {
        log.info("NOTIFICATION to={} subject=\"{}\" body=\"{}\"", notification.recipientHint(),
                notification.subject(), notification.body());
    }
}
