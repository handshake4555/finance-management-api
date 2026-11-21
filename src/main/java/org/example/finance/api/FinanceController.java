package org.example.finance.api;

import jakarta.validation.Valid;
import org.example.finance.dto.*;
import org.example.finance.mapper.FinanceMapper;
import org.example.finance.repo.TransactionRepository;
import org.example.finance.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP adapter only: validation, status codes and mapping. Finance rules live in domain services.
 */
@RestController
@RequestMapping("/api")
public class FinanceController {
    private final AccountService accounts;
    private final CategoryService categories;
    private final TransactionService transactions;
    private final BudgetService budgets;
    private final SavingsGoalService goals;
    private final NotificationService notifications;
    private final AnalyticsService analytics;
    private final RecurringTransactionService recurring;
    private final TransactionRepository transactionRepository;

    public FinanceController(AccountService a, CategoryService c, TransactionService t, BudgetService b, SavingsGoalService g, NotificationService n, AnalyticsService analytics, RecurringTransactionService recurring, TransactionRepository tr) {
        accounts = a;
        categories = c;
        transactions = t;
        budgets = b;
        goals = g;
        notifications = n;
        this.analytics = analytics;
        this.recurring = recurring;
        transactionRepository = tr;
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(accounts.create(p.getName(), r.name(), r.type(), r.currency(), r.openingBalance()));
    }

    @GetMapping("/accounts")
    public List<AccountResponse> accounts(java.security.Principal p) {
        return accounts.list(p.getName()).stream().map(FinanceMapper::toResponse).toList();
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse account(@PathVariable Long id, java.security.Principal p) {
        return FinanceMapper.toResponse(accounts.get(p.getName(), id));
    }

    @PutMapping("/accounts/{id}")
    public AccountResponse updateAccount(@PathVariable Long id, @Valid @RequestBody AccountRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(accounts.update(p.getName(), id, r.name(), r.type(), r.currency()));
    }

    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateAccount(@PathVariable Long id, java.security.Principal p) {
        accounts.deactivate(p.getName(), id);
    }

    @GetMapping("/accounts/{id}/transactions")
    public List<TransactionResponse> accountTransactions(@PathVariable Long id, java.security.Principal p) {
        accounts.get(p.getName(), id);
        return transactionRepository.findByOwnerAndAccountIdAndReversedFalseOrderByTransactionDateDesc(p.getName(), id).stream().map(FinanceMapper::toResponse).toList();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(categories.create(p.getName(), r.name(), r.type()));
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories(java.security.Principal p) {
        return categories.list(p.getName()).stream().map(FinanceMapper::toResponse).toList();
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(categories.update(p.getName(), id, r.name(), r.type()));
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCategory(@PathVariable Long id, java.security.Principal p) {
        categories.deactivate(p.getName(), id);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody TransactionRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(transactions.create(p.getName(), r.accountId(), r.categoryId(), r.type(), r.amount(), r.transactionDate(), r.description(), r.notes()));
    }

    @GetMapping("/transactions")
    public Page<TransactionResponse> transactions(@RequestParam(defaultValue = "2000-01-01") LocalDate from, @RequestParam(defaultValue = "2999-12-31") LocalDate to, @RequestParam(required = false) Long accountId, @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String type, @PageableDefault(size = 25, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable, java.security.Principal p) {
        return transactions.search(p.getName(), from, to, accountId, categoryId, type, pageable).map(FinanceMapper::toResponse);
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponse transaction(@PathVariable Long id, java.security.Principal p) {
        return FinanceMapper.toResponse(transactions.get(p.getName(), id));
    }

    @PutMapping("/transactions/{id}")
    public TransactionResponse updateTransaction(@PathVariable Long id, @Valid @RequestBody TransactionRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(transactions.update(p.getName(), id, r.accountId(), r.categoryId(), r.type(), r.amount(), r.transactionDate(), r.description(), r.notes()));
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reverseTransaction(@PathVariable Long id, java.security.Principal p) {
        transactions.reverse(p.getName(), id);
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createBudget(@Valid @RequestBody BudgetRequest r, java.security.Principal p) {
        return budgets.create(p.getName(), r.categoryId(), r.year(), r.month(), r.limitAmount());
    }

    @PostMapping("/budgets/yearly")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createAnnualBudget(@Valid @RequestBody AnnualBudgetRequest r, java.security.Principal p) {
        return budgets.createAnnual(p.getName(), r.categoryId(), r.year(), r.limitAmount());
    }

    @GetMapping("/budgets")
    public List<BudgetResponse> budgets(java.security.Principal p) {
        return budgets.list(p.getName());
    }

    @GetMapping("/budgets/{id}")
    public BudgetResponse budget(@PathVariable Long id, java.security.Principal p) {
        return budgets.get(p.getName(), id);
    }

    @PutMapping("/budgets/{id}")
    public BudgetResponse updateBudget(@PathVariable Long id, @Valid @RequestBody BudgetUpdateRequest r, java.security.Principal p) {
        return budgets.update(p.getName(), id, r.categoryId(), r.year(), r.month(), r.limitAmount());
    }

    @PutMapping("/budgets/{id}/yearly")
    public BudgetResponse updateAnnualBudget(@PathVariable Long id, @Valid @RequestBody AnnualBudgetRequest r, java.security.Principal p) {
        return budgets.updateAnnual(p.getName(), id, r.categoryId(), r.year(), r.limitAmount());
    }

    @DeleteMapping("/budgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateBudget(@PathVariable Long id, java.security.Principal p) {
        budgets.deactivate(p.getName(), id);
    }

    @PostMapping("/savings-goals")
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse createGoal(@Valid @RequestBody GoalRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(goals.create(p.getName(), r.name(), r.targetAmount(), r.targetDate()));
    }

    @GetMapping("/savings-goals")
    public List<GoalResponse> goals(java.security.Principal p) {
        return goals.list(p.getName()).stream().map(FinanceMapper::toResponse).toList();
    }

    @GetMapping("/savings-goals/{id}")
    public GoalResponse goal(@PathVariable Long id, java.security.Principal p) {
        return FinanceMapper.toResponse(goals.get(p.getName(), id));
    }

    @PutMapping("/savings-goals/{id}")
    public GoalResponse updateGoal(@PathVariable Long id, @Valid @RequestBody GoalRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(goals.update(p.getName(), id, r.name(), r.targetAmount(), r.targetDate()));
    }

    @DeleteMapping("/savings-goals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateGoal(@PathVariable Long id, java.security.Principal p) {
        goals.deactivate(p.getName(), id);
    }

    @GetMapping("/savings-goals/{id}/contributions")
    public List<TransactionResponse> contributions(@PathVariable Long id, java.security.Principal p) {
        return goals.contributions(p.getName(), id).stream().map(FinanceMapper::toResponse).toList();
    }

    @PostMapping("/savings-goals/{id}/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse contribute(@PathVariable Long id, @Valid @RequestBody ContributionRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(goals.contribute(p.getName(), id, r.accountId(), r.amount(), r.date()));
    }

    @PostMapping("/recurring-transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringResponse createRecurring(@Valid @RequestBody RecurringRequest r, java.security.Principal p) {
        return FinanceMapper.toResponse(recurring.create(p.getName(), r.accountId(), r.categoryId(), r.type(), r.amount(), r.frequency(), r.startDate(), r.endDate(), r.description()));
    }

    @GetMapping("/recurring-transactions")
    public List<RecurringResponse> recurring(java.security.Principal p) {
        return recurring.list(p.getName()).stream().map(FinanceMapper::toResponse).toList();
    }

    @GetMapping("/recurring-transactions/{id}/executions")
    public List<RecurringExecutionResponse> recurringHistory(@PathVariable Long id, java.security.Principal p) {
        return recurring.history(p.getName(), id).stream().map(FinanceMapper::toResponse).toList();
    }

    @PatchMapping("/recurring-transactions/{id}/pause")
    public RecurringResponse pauseRecurring(@PathVariable Long id, java.security.Principal p) {
        return FinanceMapper.toResponse(recurring.pause(p.getName(), id, true));
    }

    @PatchMapping("/recurring-transactions/{id}/resume")
    public RecurringResponse resumeRecurring(@PathVariable Long id, java.security.Principal p) {
        return FinanceMapper.toResponse(recurring.pause(p.getName(), id, false));
    }

    @DeleteMapping("/recurring-transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateRecurring(@PathVariable Long id, java.security.Principal p) {
        recurring.deactivate(p.getName(), id);
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications(java.security.Principal p) {
        return notifications.list(p.getName()).stream().map(FinanceMapper::toResponse).toList();
    }

    @GetMapping("/notifications/unread")
    public List<NotificationResponse> unread(java.security.Principal p) {
        return notifications.unread(p.getName()).stream().map(FinanceMapper::toResponse).toList();
    }

    @PatchMapping("/notifications/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, java.security.Principal p) {
        return FinanceMapper.toResponse(notifications.markRead(p.getName(), id));
    }

    @PatchMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(java.security.Principal p) {
        notifications.markAllRead(p.getName());
    }

    @DeleteMapping("/notifications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable Long id, java.security.Principal p) {
        notifications.delete(p.getName(), id);
    }

    @GetMapping("/dashboard/summary")
    public Map<String, Object> summary(@RequestParam(defaultValue = "2000-01-01") LocalDate from, @RequestParam(defaultValue = "2999-12-31") LocalDate to, java.security.Principal p) {
        return analytics.summary(p.getName(), from, to);
    }

    @GetMapping("/dashboard/monthly")
    public Map<String, BigDecimal> monthly(@RequestParam int year, java.security.Principal p) {
        return analytics.monthlyCashFlow(p.getName(), year);
    }

    @GetMapping("/reports/monthly")
    public Map<String, Object> monthlyReport(@RequestParam int year, @RequestParam int month, java.security.Principal p) {
        return analytics.monthlyReport(p.getName(), year, month);
    }

    @GetMapping("/reports/yearly")
    public Map<String, Object> yearlyReport(@RequestParam int year, java.security.Principal p) {
        return analytics.yearlyReport(p.getName(), year);
    }

    @GetMapping("/reports/expenses-by-account")
    public Map<Long, BigDecimal> expenseReportByAccount(@RequestParam(defaultValue = "2000-01-01") LocalDate from, @RequestParam(defaultValue = "2999-12-31") LocalDate to, java.security.Principal p) {
        return analytics.expensesByAccount(p.getName(), from, to);
    }

    @GetMapping("/dashboard/expenses-by-category")
    public Map<String, BigDecimal> expensesByCategory(@RequestParam(defaultValue = "2000-01-01") LocalDate from, @RequestParam(defaultValue = "2999-12-31") LocalDate to, java.security.Principal p) {
        return analytics.expensesByCategory(p.getName(), from, to);
    }

    @GetMapping(value = "/reports/transactions.csv", produces = "text/csv")
    public String export(@RequestParam(defaultValue = "2000-01-01") LocalDate from, @RequestParam(defaultValue = "2999-12-31") LocalDate to, java.security.Principal p) {
        var page = transactions.search(p.getName(), from, to, null, null, null, Pageable.unpaged());
        StringBuilder csv = new StringBuilder("id,date,type,amount,accountId,categoryId,description\n");
        page.forEach(t -> csv.append(t.id).append(',').append(t.transactionDate).append(',').append(t.type).append(',').append(t.amount).append(',').append(t.accountId).append(',').append(t.categoryId == null ? "" : t.categoryId).append(',').append('"').append(Objects.toString(t.description, "").replace("\"", "\"\"")).append('"').append('\n'));
        return csv.toString();
    }
}
