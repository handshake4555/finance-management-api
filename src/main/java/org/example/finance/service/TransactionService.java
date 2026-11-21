package org.example.finance.service;

import org.example.finance.model.FinanceTransaction;
import org.example.finance.model.FinancialAccount;
import org.example.finance.repo.CategoryRepository;
import org.example.finance.repo.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final AccountService accountService;
    private final CategoryRepository categories;
    private final BudgetService budgetService;

    public TransactionService(TransactionRepository t, AccountService a, CategoryRepository c, BudgetService b) {
        transactions = t;
        accountService = a;
        categories = c;
        budgetService = b;
    }

    @Transactional
    public FinanceTransaction create(String owner, Long accountId, Long categoryId, String type, BigDecimal amount, LocalDate date, String description, String notes) {
        validate(owner, categoryId, type, amount);
        var account = accountService.lockOwned(owner, accountId);
        ensureActive(account);
        apply(account, type, amount);
        var tx = transactions.save(new FinanceTransaction(owner, account.id, categoryId, type, amount, date, description, notes));
        budgetService.evaluateExpense(tx);
        return tx;
    }

    @Transactional
    public FinanceTransaction update(String owner, Long id, Long accountId, Long categoryId, String type, BigDecimal amount, LocalDate date, String description, String notes) {
        validate(owner, categoryId, type, amount);
        var tx = get(owner, id);
        if (tx.reversed)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reversed transactions cannot be edited");
        var old = accountService.lockOwned(owner, tx.accountId);
        var target = tx.accountId.equals(accountId) ? old : accountService.lockOwned(owner, accountId);
        old.balance = old.balance.subtract(delta(old, tx.type, tx.amount));
        ensureActive(target);
        apply(target, type, amount);
        tx.accountId = target.id;
        tx.categoryId = categoryId;
        tx.type = type;
        tx.amount = amount;
        tx.transactionDate = date;
        tx.description = description;
        tx.notes = notes;
        budgetService.evaluateExpense(tx);
        return tx;
    }

    @Transactional
    public void reverse(String owner, Long id) {
        var tx = get(owner, id);
        if (tx.reversed) return;
        var account = accountService.lockOwned(owner, tx.accountId);
        account.balance = account.balance.subtract(delta(account, tx.type, tx.amount));
        tx.reversed = true;
    }

    public FinanceTransaction get(String owner, Long id) {
        return transactions.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public Page<FinanceTransaction> search(String owner, LocalDate from, LocalDate to, Long accountId, Long categoryId, String type, Pageable pageable) {
        if (from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must not be after to");
        if (type != null && !List.of("INCOME", "EXPENSE").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported transaction type");
        if (accountId != null) accountService.get(owner, accountId);
        return transactions.search(owner, from, to, accountId, categoryId, type, pageable);
    }

    private void validate(String owner, Long categoryId, String type, BigDecimal amount) {
        if (!List.of("INCOME", "EXPENSE").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type must be INCOME or EXPENSE");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() > 19)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive and use at most two decimals");
        if (categoryId != null) {
            var c = categories.findByIdAndOwner(categoryId, owner).or(() -> categories.findByIdAndOwner(categoryId, "__SYSTEM__")).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            if (!c.active || !c.type.equals(type))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category type does not match transaction");
        }
    }

    private void ensureActive(FinancialAccount a) {
        if (!a.active) throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is inactive");
    }

    private void apply(FinancialAccount a, String type, BigDecimal amount) {
        BigDecimal change = delta(a, type, amount);
        if (!a.type.equals("CREDIT_CARD") && a.balance.add(change).signum() < 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient account balance");
        a.balance = a.balance.add(change);
    }

    private BigDecimal delta(FinancialAccount a, String type, BigDecimal amount) {
        BigDecimal change = type.equals("INCOME") ? amount : amount.negate();
        return a.type.equals("CREDIT_CARD") ? change.negate() : change;
    }
}
