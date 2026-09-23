package com.ledgerflow.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

/**
 * One row per {@code Idempotency-Key} seen on a mutating endpoint. The primary key is the
 * client-supplied key itself, so a concurrent INSERT race is decided by the database's unique
 * constraint, not by application logic — see
 * {@link com.ledgerflow.account.service.IdempotencyService} for how the "first writer wins,
 * everyone else waits for the result" protocol works.
 *
 * <p>Implements {@link Persistable} so {@code JpaRepository.save()} always issues a real
 * INSERT for a freshly-constructed instance instead of Spring Data JPA's default behavior for
 * manually-assigned {@code @Id} entities, which is to treat a non-null id as "already exists"
 * and silently {@code merge()} (upsert) rather than {@code persist()}. Without this, two
 * concurrent callers racing on the same key would both "succeed" via merge instead of one of
 * them hitting the primary-key uniqueness constraint — which is the entire mechanism this
 * table relies on to pick exactly one winner.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyRecord implements Persistable<String> {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Transient
    private boolean isNew = true;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "in_progress", nullable = false)
    private boolean inProgress = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected IdempotencyKeyRecord() {
    }

    public IdempotencyKeyRecord(String idempotencyKey, String requestHash) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    public void complete(int status, String body) {
        this.responseStatus = status;
        this.responseBody = body;
        this.inProgress = false;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public String getId() {
        return idempotencyKey;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
