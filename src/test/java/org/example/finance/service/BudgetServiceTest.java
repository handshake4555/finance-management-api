package org.example.finance.service;

import org.example.finance.model.Budget;
import org.example.finance.model.FinanceTransaction;
import org.example.finance.repo.BudgetRepository;
import org.example.finance.repo.CategoryRepository;
import org.example.finance.repo.NotificationRepository;
import org.example.finance.repo.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetServiceTest {
    @Test
    void computesUtilizationFromTransactionsAndCreatesOneThresholdNotice() {
        var budgets = mock(BudgetRepository.class);
        var ledger = mock(TransactionRepository.class);
        var cats = mock(CategoryRepository.class);
        var notices = mock(NotificationRepository.class);
        var service = new BudgetService(budgets, ledger, cats, notices);
        var budget = new Budget("alice", 2L, 2026, 11, new BigDecimal("8000.00"));
        budget.id = 5L;
        when(budgets.findByOwnerAndActiveTrue("alice")).thenReturn(List.of(budget));
        var expense = new FinanceTransaction("alice", 1L, 2L, "EXPENSE", new BigDecimal("6400.00"), LocalDate.of(2026, 11, 10), "Food", null);
        when(ledger.findByOwnerAndReversedFalseAndTransactionDateBetween("alice", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(List.of(expense));
        when(notices.existsByEventKey(any())).thenReturn(false);
        var result = service.view(budget, "alice");
        assertEquals(new BigDecimal("80.00"), result.utilizationPercent());
        service.evaluateExpense(expense);
        verify(notices).save(argThat(n -> n.owner.equals("alice") && n.type.equals("BUDGET") && n.message.contains("80%")));
    }

    @Test
    void annualBudgetUsesTheWholeCalendarYear() {
        var budgets = mock(BudgetRepository.class);
        var ledger = mock(TransactionRepository.class);
        var cats = mock(CategoryRepository.class);
        var notices = mock(NotificationRepository.class);
        var service = new BudgetService(budgets, ledger, cats, notices);
        var annual = new Budget("alice", null, 2026, 1, new BigDecimal("20000.00"), "YEARLY");
        annual.id = 9L;
        var january = new FinanceTransaction("alice", 1L, null, "EXPENSE", new BigDecimal("4000.00"), LocalDate.of(2026, 1, 3), "Rent", null);
        var december = new FinanceTransaction("alice", 1L, null, "EXPENSE", new BigDecimal("5000.00"), LocalDate.of(2026, 12, 3), "Travel", null);
        when(ledger.findByOwnerAndReversedFalseAndTransactionDateBetween("alice", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).thenReturn(List.of(january, december));
        assertEquals(new BigDecimal("9000.00"), service.view(annual, "alice").spent());
    }
}
