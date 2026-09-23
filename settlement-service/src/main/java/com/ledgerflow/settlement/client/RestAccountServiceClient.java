package com.ledgerflow.settlement.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to account-service's {@code POST /api/v1/transfers} synchronously. Wrapped in
 * Resilience4j retry + circuit breaker ("settlementClient", configured in application.yml) so
 * transient account-service unavailability doesn't immediately fail (and compensate) an
 * otherwise-healthy saga.
 */
@Component
public class RestAccountServiceClient implements AccountServiceClient {

    private final RestClient restClient;

    public RestAccountServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Retry(name = "settlementClient")
    @CircuitBreaker(name = "settlementClient")
    public TransferResult transfer(String idempotencyKey, UUID fromAccountId, UUID toAccountId, BigDecimal amount,
            String currency, String reason) {
        try {
            Map<?, ?> body = restClient.post()
                    .uri("/api/v1/transfers")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(Map.of(
                            "fromAccountId", fromAccountId,
                            "toAccountId", toAccountId,
                            "amount", amount,
                            "currency", currency,
                            "reason", reason == null ? "" : reason))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        throw new AccountServiceCallException("account-service rejected transfer: "
                                + resp.getStatusCode(), null);
                    })
                    .body(Map.class);
            if (body == null || body.get("transferId") == null) {
                throw new AccountServiceCallException("account-service returned an empty transfer response", null);
            }
            return new TransferResult(UUID.fromString(String.valueOf(body.get("transferId"))));
        } catch (AccountServiceCallException e) {
            throw e;
        } catch (Exception e) {
            throw new AccountServiceCallException("Failed to call account-service", e);
        }
    }
}
