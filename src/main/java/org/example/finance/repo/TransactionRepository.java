package org.example.finance.repo;

import org.example.finance.model.FinanceTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<FinanceTransaction, Long> {
    Optional<FinanceTransaction> findByIdAndOwner(Long id, String owner);

    @Query("select t from FinanceTransaction t where t.owner=:owner and t.reversed=false and t.transactionDate between :from and :to and (:accountId is null or t.accountId=:accountId) and (:categoryId is null or t.categoryId=:categoryId) and (:type is null or t.type=:type)")
    Page<FinanceTransaction> search(@Param("owner") String owner, @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("accountId") Long accountId, @Param("categoryId") Long categoryId, @Param("type") String type, Pageable p);

    List<FinanceTransaction> findByOwnerAndReversedFalseAndTransactionDateBetween(String owner, LocalDate from, LocalDate to);

    List<FinanceTransaction> findByOwnerAndAccountIdAndReversedFalseOrderByTransactionDateDesc(String owner, Long accountId);
}
