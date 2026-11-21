package org.example.finance.repo;

import org.example.finance.model.FinanceNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<FinanceNotification, Long> {
    List<FinanceNotification> findByOwnerOrderByCreatedAtDesc(String owner);

    List<FinanceNotification> findByOwnerAndReadFalseOrderByCreatedAtDesc(String owner);

    Optional<FinanceNotification> findByIdAndOwner(Long id, String owner);

    boolean existsByEventKey(String key);
}
