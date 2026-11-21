package org.example.finance.repo;

import org.example.finance.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByOwnerAndActiveTrue(String owner);

    Optional<Budget> findByIdAndOwner(Long id, String owner);
}
