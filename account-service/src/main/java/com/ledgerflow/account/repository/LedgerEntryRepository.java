package com.ledgerflow.account.repository;

import com.ledgerflow.account.domain.LedgerEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    List<LedgerEntry> findByTransferId(UUID transferId);

    @Query("select coalesce(sum(case when e.entryType = com.ledgerflow.account.domain.EntryType.CREDIT "
            + "then e.amount else -e.amount end), 0) from LedgerEntry e where e.accountId = :accountId")
    java.math.BigDecimal sumSignedAmountByAccountId(UUID accountId);

    @Query("select coalesce(sum(case when e.entryType = com.ledgerflow.account.domain.EntryType.CREDIT "
            + "then e.amount else -e.amount end), 0) from LedgerEntry e")
    java.math.BigDecimal sumSignedAmountAll();
}
