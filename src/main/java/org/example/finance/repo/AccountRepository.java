package org.example.finance.repo;

import jakarta.persistence.LockModeType;
import org.example.finance.model.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findByOwnerAndActiveTrue(String owner);

    Optional<FinancialAccount> findByIdAndOwner(Long id, String owner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from FinancialAccount a where a.id=:id and a.owner=:owner")
    Optional<FinancialAccount> lockOwned(@Param("id") Long id, @Param("owner") String owner);
}
