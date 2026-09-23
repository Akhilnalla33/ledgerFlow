package com.ledgerflow.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.ledgerflow.events.AccountBalanceChangedEvent;
import com.ledgerflow.events.SettlementStatusChangedEvent;
import com.ledgerflow.notification.domain.Notification;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventNotificationServiceTest {

    @Mock
    private NotificationSender sender;

    @Test
    void balanceChangedEventProducesANotificationMentioningTheNewBalance() {
        UUID accountId = UUID.randomUUID();
        AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(UUID.randomUUID(), Instant.now(), "corr",
                accountId, UUID.randomUUID(), UUID.randomUUID(), "CREDIT", new BigDecimal("50.00"), "USD",
                new BigDecimal("150.00"), "payout");

        new EventNotificationService(sender).onBalanceChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().body()).contains("150.00");
        assertThat(captor.getValue().recipientHint()).isEqualTo(accountId.toString());
    }

    @Test
    void settlementFailureEventMentionsTheFailureReason() {
        SettlementStatusChangedEvent event = new SettlementStatusChangedEvent(UUID.randomUUID(), Instant.now(),
                "corr", UUID.randomUUID(), "COMPENSATED", "insufficient funds", List.of(UUID.randomUUID()));

        new EventNotificationService(sender).onSettlementStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().body()).contains("insufficient funds");
        assertThat(captor.getValue().subject()).contains("COMPENSATED");
    }
}
