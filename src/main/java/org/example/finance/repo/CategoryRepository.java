package org.example.finance.repo;

import org.example.finance.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByOwnerAndActiveTrue(String owner);

    Optional<Category> findByIdAndOwner(Long id, String owner);

    boolean existsByOwnerAndNameAndType(String owner, String name, String type);
}
