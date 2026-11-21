package org.example.finance.service;

import org.example.finance.model.FinanceNotification;
import org.example.finance.model.FinanceTransaction;
import org.example.finance.model.SavingsGoal;
import org.example.finance.repo.GoalRepository;
import org.example.finance.repo.NotificationRepository;
import org.example.finance.repo.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class SavingsGoalService {
    private final GoalRepository goals;
    private final AccountService accounts;
    private final TransactionRepository transactions;
    private final NotificationRepository notifications;

    public SavingsGoalService(GoalRepository g, AccountService a, TransactionRepository t, NotificationRepository n) {
        goals = g;
        accounts = a;
        transactions = t;
        notifications = n;
    }

    public SavingsGoal create(String owner, String name, BigDecimal target, LocalDate date) {
        if (target == null || target.signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target must be positive");
        return goals.save(new SavingsGoal(owner, name, target, date));
    }

    public List<SavingsGoal> list(String owner) {
        return goals.findByOwnerAndActiveTrue(owner);
    }

    public SavingsGoal get(String owner, Long id) {
        return owned(owner, id);
    }

    @Transactional
    public SavingsGoal update(String owner, Long id, String name, BigDecimal target, LocalDate date) {
        var goal = owned(owner, id);
        if (goal.currentAmount.compareTo(target) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Target cannot be below contributions already made");
        goal.name = name;
        goal.targetAmount = target;
        goal.targetDate = date;
        goal.active = goal.currentAmount.compareTo(target) < 0;
        return goal;
    }

    @Transactional
    public void deactivate(String owner, Long id) {
        owned(owner, id).active = false;
    }

    @Transactional
    public SavingsGoal contribute(String owner, Long goalId, Long accountId, BigDecimal amount, LocalDate date) {
        var goal = goals.findByIdAndOwner(goalId, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
        var account = accounts.lockOwned(owner, accountId);
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contribution must be positive");
        if (!account.active || account.balance.compareTo(amount) < 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient account balance");
        account.balance = account.balance.subtract(amount);
        goal.currentAmount = goal.currentAmount.add(amount);
        transactions.save(new FinanceTransaction(owner, account.id, null, "EXPENSE", amount, date, "Savings contribution: " + goal.name, "Goal " + goal.id));
        if (goal.currentAmount.compareTo(goal.targetAmount) >= 0) {
            goal.active = false;
            String key = "goal:" + goal.id + ":completed";
            if (!notifications.existsByEventKey(key))
                notifications.save(new FinanceNotification(owner, "SAVINGS", "Savings goal completed: " + goal.name, key));
        }
        return goal;
    }

    public List<FinanceTransaction> contributions(String owner, Long goalId) {
        var goal = goals.findByIdAndOwner(goalId, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
        return transactions.findByOwnerAndReversedFalseAndTransactionDateBetween(owner, LocalDate.of(2000, 1, 1), LocalDate.of(2999, 12, 31)).stream().filter(t -> Objects.equals(t.notes, "Goal " + goal.id)).toList();
    }

    private SavingsGoal owned(String owner, Long id) {
        return goals.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
    }
}
