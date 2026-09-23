package com.ledgerflow.account.repository;

import com.ledgerflow.account.domain.Account;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * No custom locking query here on purpose: {@link Account#getVersion()} is a JPA
 * {@code @Version} column, so a plain {@code findById} + mutate + {@code save} within a
 * transaction is enough — Hibernate includes {@code WHERE version = ?} on the UPDATE and
 * throws {@link org.springframework.orm.ObjectOptimisticLockingFailureException} if another
 * transaction committed first. {@link com.ledgerflow.account.service.TransferService} retries
 * on that exception. See ADR-0002 for why optimistic locking was chosen over
 * {@code SELECT ... FOR UPDATE}.
 */
public interface AccountRepository extends JpaRepository<Account, UUID> {
}
