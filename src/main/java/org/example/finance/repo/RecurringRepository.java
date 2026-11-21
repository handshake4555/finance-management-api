package org.example.finance.repo;

import org.example.finance.model.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByActiveTrueAndPausedFalseAndNextExecutionDateLessThanEqual(LocalDate date);

    Optional<RecurringTransaction> findByIdAndOwner(Long id, String owner);

    List<RecurringTransaction> findByOwnerOrderByNextExecutionDateAsc(String owner);
}
