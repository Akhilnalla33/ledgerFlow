package com.ledgerflow.account.repository;

import com.ledgerflow.account.domain.IdempotencyKeyRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, String> {

    Optional<IdempotencyKeyRecord> findByIdempotencyKey(String idempotencyKey);
}
