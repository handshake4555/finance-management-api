package org.example.finance.repo;

import org.example.finance.model.RecurringExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringExecutionRepository extends JpaRepository<RecurringExecution, Long> {
    List<RecurringExecution> findByRecurringIdOrderByExecutionDateDesc(Long id);

    boolean existsByRecurringIdAndExecutionDate(Long id, java.time.LocalDate date);
}
