package org.example.finance.repo;

import org.example.finance.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByOwnerAndActiveTrue(String owner);

    Optional<SavingsGoal> findByIdAndOwner(Long id, String owner);
}
