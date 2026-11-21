package org.example.finance.service;

import jakarta.annotation.PostConstruct;
import org.example.finance.model.Category;
import org.example.finance.repo.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categories;

    public CategoryService(CategoryRepository c) {
        categories = c;
    }

    private static final String SYSTEM = "__SYSTEM__";

    @PostConstruct
    public void seedDefaults() {
        for (String name : List.of("Food", "Groceries", "Transportation", "Rent", "Utilities", "Shopping", "Healthcare", "Education", "Entertainment", "Travel"))
            seed(name, "EXPENSE");
        for (String name : List.of("Salary", "Business", "Freelance", "Investments", "Other income"))
            seed(name, "INCOME");
    }

    private void seed(String name, String type) {
        if (!categories.existsByOwnerAndNameAndType(SYSTEM, name, type))
            categories.save(new Category(SYSTEM, name, type));
    }

    public List<Category> list(String owner) {
        List<Category> result = new ArrayList<>(categories.findByOwnerAndActiveTrue(SYSTEM));
        result.addAll(categories.findByOwnerAndActiveTrue(owner));
        return result;
    }

    public Category create(String owner, String name, String type) {
        validate(type);
        return categories.save(new Category(owner, name, type));
    }

    @Transactional
    public Category update(String owner, Long id, String name, String type) {
        validate(type);
        var c = owned(owner, id);
        c.name = name;
        c.type = type;
        return c;
    }

    @Transactional
    public void deactivate(String owner, Long id) {
        owned(owner, id).active = false;
    }

    public Category get(String owner, Long id) {
        return categories.findByIdAndOwner(id, owner).or(() -> categories.findByIdAndOwner(id, SYSTEM)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private Category owned(String owner, Long id) {
        return categories.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or system category is read-only"));
    }

    private void validate(String type) {
        if (!List.of("INCOME", "EXPENSE").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category type must be INCOME or EXPENSE");
    }
}
