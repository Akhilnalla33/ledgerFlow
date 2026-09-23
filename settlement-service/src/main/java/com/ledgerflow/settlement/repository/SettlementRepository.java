package com.ledgerflow.settlement.repository;

import com.ledgerflow.settlement.domain.Settlement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
}
