# notification-service

Consumes `AccountBalanceChanged` and `SettlementStatusChanged` Kafka events and sends
notifications. The sender is a logging stub (`LoggingNotificationSender`) behind a
`NotificationSender` interface — no real email/webhook provider is integrated, by design (see
the [root README](../README.md)).

## Running locally

```bash
mvn -pl notification-service -am spring-boot:run
```

## Tests

```bash
mvn -pl notification-service -am verify
```
