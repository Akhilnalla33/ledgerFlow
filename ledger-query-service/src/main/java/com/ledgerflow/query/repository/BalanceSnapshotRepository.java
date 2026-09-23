package com.ledgerflow.query.repository;

import com.ledgerflow.query.domain.BalanceSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceSnapshotRepository extends JpaRepository<BalanceSnapshot, UUID> {
}
