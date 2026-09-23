package com.ledgerflow.query.repository;

import com.ledgerflow.query.domain.StatementEntry;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementEntryRepository extends JpaRepository<StatementEntry, UUID> {

    Page<StatementEntry> findByAccountIdOrderByOccurredAtDesc(UUID accountId, Pageable pageable);
}
