package org.example.finance.service;

import org.example.finance.dto.BudgetResponse;
import org.example.finance.model.Budget;
import org.example.finance.model.FinanceNotification;
import org.example.finance.model.FinanceTransaction;
import org.example.finance.repo.BudgetRepository;
import org.example.finance.repo.CategoryRepository;
import org.example.finance.repo.NotificationRepository;
import org.example.finance.repo.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {
    private final BudgetRepository budgets;
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final NotificationRepository notifications;

    public BudgetService(BudgetRepository b, TransactionRepository t, CategoryRepository c, NotificationRepository n) {
        budgets = b;
        transactions = t;
        categories = c;
        notifications = n;
    }

    public List<BudgetResponse> list(String owner) {
        return budgets.findByOwnerAndActiveTrue(owner).stream().map(b -> view(b, owner)).toList();
    }

    public BudgetResponse get(String owner, Long id) {
        return view(budgets.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found")), owner);
    }

    public BudgetResponse create(String owner, Long categoryId, int year, int month, BigDecimal limit) {
        if (limit == null || limit.signum() <= 0 || limit.scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget limit must be positive");
        if (month < 1 || month > 12)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12");
        validateExpenseCategory(owner, categoryId);
        return view(budgets.save(new Budget(owner, categoryId, year, month, limit)), owner);
    }

    public BudgetResponse createAnnual(String owner, Long categoryId, int year, BigDecimal limit) {
        if (limit == null || limit.signum() <= 0 || limit.scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget limit must be positive");
        validateExpenseCategory(owner, categoryId);
        return view(budgets.save(new Budget(owner, categoryId, year, 1, limit, "YEARLY")), owner);
    }

    @Transactional
    public BudgetResponse update(String owner, Long id, Long categoryId, int year, int month, BigDecimal limit) {
        var budget = budgets.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        if (month < 1 || month > 12 || limit == null || limit.signum() <= 0 || limit.scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget period or limit");
        validateExpenseCategory(owner, categoryId);
        budget.categoryId = categoryId;
        budget.year = year;
        budget.month = month;
        budget.period = "MONTHLY";
        budget.limitAmount = limit;
        return view(budget, owner);
    }

    @Transactional
    public BudgetResponse updateAnnual(String owner, Long id, Long categoryId, int year, BigDecimal limit) {
        var budget = budgets.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        if (limit == null || limit.signum() <= 0 || limit.scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Annual limit must be positive");
        validateExpenseCategory(owner, categoryId);
        budget.categoryId = categoryId;
        budget.year = year;
        budget.period = "YEARLY";
        budget.month = 1;
        budget.limitAmount = limit;
        return view(budget, owner);
    }

    @Transactional
    public void deactivate(String owner, Long id) {
        budgets.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found")).active = false;
    }

    public void evaluateExpense(FinanceTransaction tx) {
        if (!"EXPENSE".equals(tx.type)) return;
        for (var b : budgets.findByOwnerAndActiveTrue(tx.owner)) {
            if (b.year != tx.transactionDate.getYear() || ("MONTHLY".equals(b.period) && b.month != tx.transactionDate.getMonthValue()) || (b.categoryId != null && !b.categoryId.equals(tx.categoryId)))
                continue;
            BigDecimal spent = spent(tx.owner, b);
            int reached = spent.compareTo(b.limitAmount) >= 0 ? 100 : spent.multiply(BigDecimal.valueOf(100)).compareTo(b.limitAmount.multiply(BigDecimal.valueOf(80))) >= 0 ? 80 : 0;
            if (reached == 0) continue;
            String key = "budget:" + b.id + ":" + b.year + ":" + b.period + ":" + b.month + ":" + reached;
            if (!notifications.existsByEventKey(key))
                notifications.save(new FinanceNotification(tx.owner, "BUDGET", reached == 100 ? "Budget exceeded" : "Budget reached 80%", key));
        }
    }

    public BudgetResponse view(Budget b, String owner) {
        BigDecimal spent = spent(owner, b);
        return new BudgetResponse(b.id, b.categoryId == null ? 0 : b.categoryId, b.year, b.month, b.period, b.limitAmount, spent, b.limitAmount.subtract(spent), spent.multiply(BigDecimal.valueOf(100)).divide(b.limitAmount, 2, RoundingMode.HALF_UP), spent.compareTo(b.limitAmount) > 0);
    }

    private BigDecimal spent(String owner, Budget b) {
        LocalDate start = "YEARLY".equals(b.period) ? LocalDate.of(b.year, 1, 1) : LocalDate.of(b.year, b.month, 1);
        LocalDate end = "YEARLY".equals(b.period) ? LocalDate.of(b.year, 12, 31) : start.withDayOfMonth(start.lengthOfMonth());
        return transactions.findByOwnerAndReversedFalseAndTransactionDateBetween(owner, start, end).stream().filter(t -> t.type.equals("EXPENSE") && (b.categoryId == null || b.categoryId.equals(t.categoryId))).map(t -> t.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateExpenseCategory(String owner, Long categoryId) {
        if (categoryId == null) return;
        var c = categories.findByIdAndOwner(categoryId, owner).or(() -> categories.findByIdAndOwner(categoryId, "__SYSTEM__")).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        if (!c.type.equals("EXPENSE"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget category must be an expense category");
    }
}
