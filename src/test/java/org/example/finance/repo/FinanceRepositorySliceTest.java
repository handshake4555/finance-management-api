package org.example.finance.repo;

import org.example.finance.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FinanceRepositorySliceTest {
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired BudgetRepository budgets;
    @Autowired GoalRepository goals;
    @Autowired NotificationRepository notifications;
    @Autowired RecurringRepository recurring;
    @Autowired RecurringExecutionRepository executions;
    @Autowired TransactionRepository transactions;

    @Test void ownerAndActiveQueriesRespectTheirFilters() {
        var active = accounts.save(new FinancialAccount("alice", "Cash", "CASH", "INR", BigDecimal.TEN));
        var inactive = accounts.save(new FinancialAccount("alice", "Old", "CASH", "INR", BigDecimal.ZERO)); inactive.active = false;
        accounts.save(new FinancialAccount("bob", "Other", "CASH", "INR", BigDecimal.ONE));
        assertEquals(1, accounts.findByOwnerAndActiveTrue("alice").size());
        assertTrue(accounts.findByIdAndOwner(active.id, "alice").isPresent());
        assertTrue(accounts.findByIdAndOwner(active.id, "bob").isEmpty());
        var category = categories.save(new Category("alice", "Food", "EXPENSE"));
        categories.save(new Category("alice", "Old", "EXPENSE")).active = false;
        assertEquals(1, categories.findByOwnerAndActiveTrue("alice").size());
        assertTrue(categories.existsByOwnerAndNameAndType("alice", "Food", "EXPENSE"));
        assertTrue(categories.findByIdAndOwner(category.id, "bob").isEmpty());
        var budget = budgets.save(new Budget("alice", category.id, 2026, 11, BigDecimal.TEN));
        assertEquals(budget.id, budgets.findByIdAndOwner(budget.id, "alice").orElseThrow().id);
        assertEquals(1, budgets.findByOwnerAndActiveTrue("alice").size());
        var goal = goals.save(new SavingsGoal("alice", "Trip", BigDecimal.TEN, null));
        assertEquals(goal.id, goals.findByIdAndOwner(goal.id, "alice").orElseThrow().id);
        assertEquals(1, goals.findByOwnerAndActiveTrue("alice").size());
    }

    @Test void notificationsRecurringAndTransactionQueriesWork() {
        var notice = notifications.save(new FinanceNotification("alice", "BUDGET", "Near limit", "event-1"));
        assertTrue(notifications.existsByEventKey("event-1"));
        assertEquals(notice.id, notifications.findByIdAndOwner(notice.id, "alice").orElseThrow().id);
        assertEquals(1, notifications.findByOwnerAndReadFalseOrderByCreatedAtDesc("alice").size());
        notice.read = true;
        assertTrue(notifications.findByOwnerAndReadFalseOrderByCreatedAtDesc("alice").isEmpty());
        LocalDate today = LocalDate.of(2026, 11, 21);
        var plan = recurring.save(new RecurringTransaction("alice", 2L, null, "INCOME", BigDecimal.TEN, "MONTHLY", today, null, "salary"));
        assertEquals(1, recurring.findByActiveTrueAndPausedFalseAndNextExecutionDateLessThanEqual(today).size());
        assertEquals(plan.id, recurring.findByIdAndOwner(plan.id, "alice").orElseThrow().id);
        var execution = executions.save(new RecurringExecution(plan.id, today, "SUCCESS", 10L, null));
        assertTrue(executions.existsByRecurringIdAndExecutionDate(plan.id, today));
        assertEquals(execution.id, executions.findByRecurringIdOrderByExecutionDateDesc(plan.id).getFirst().id);
        var tx = transactions.save(new FinanceTransaction("alice", 2L, 3L, "EXPENSE", BigDecimal.TEN, today, "lunch", null));
        assertTrue(transactions.findByIdAndOwner(tx.id, "alice").isPresent());
        assertEquals(1, transactions.search("alice", today, today, 2L, 3L, "EXPENSE", org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        assertEquals(1, transactions.findByOwnerAndReversedFalseAndTransactionDateBetween("alice", today, today).size());
        assertEquals(tx.id, transactions.findByOwnerAndAccountIdAndReversedFalseOrderByTransactionDateDesc("alice", 2L).getFirst().id);
    }
}
