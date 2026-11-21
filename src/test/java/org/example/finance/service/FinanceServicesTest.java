package org.example.finance.service;

import org.example.finance.model.*;
import org.example.finance.repo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServicesTest {
    @Mock
    AccountRepository accounts;
    @Mock
    CategoryRepository categories;
    @Mock
    TransactionRepository transactions;
    @Mock
    NotificationRepository notifications;
    @Mock
    GoalRepository goals;
    @Mock
    RecurringRepository recurring;
    @Mock
    RecurringExecutionRepository executions;
    @Mock
    RecurringExecutionWorker worker;

    @Test
    void accountServiceCreatesDefaultsAndRejectsInvalidTypes() {
        var service = new AccountService(accounts);
        when(accounts.save(any())).thenAnswer(call -> call.getArgument(0));
        var created = service.create("alice", "Cash", "CASH", null, null);
        assertEquals("INR", created.currency);
        assertEquals(BigDecimal.ZERO, created.balance);
        assertThrows(ResponseStatusException.class, () -> service.create("alice", "bad", "INVALID", "INR", BigDecimal.ZERO));
        verify(accounts, times(1)).save(any());
    }

    @Test
    void categoryServiceCombinesSystemAndUserCategoriesAndProtectsSystemCategories() {
        var service = new CategoryService(categories);
        var system = new Category("__SYSTEM__", "Food", "EXPENSE");
        system.id = 1L;
        var personal = new Category("alice", "Books", "EXPENSE");
        personal.id = 2L;
        when(categories.findByOwnerAndActiveTrue("__SYSTEM__")).thenReturn(List.of(system));
        when(categories.findByOwnerAndActiveTrue("alice")).thenReturn(List.of(personal));
        when(categories.findByIdAndOwner(1L, "alice")).thenReturn(Optional.empty());
        when(categories.findByIdAndOwner(1L, "__SYSTEM__")).thenReturn(Optional.of(system));
        assertEquals(2, service.list("alice").size());
        assertSame(system, service.get("alice", 1L));
        assertThrows(ResponseStatusException.class, () -> service.update("alice", 1L, "Changed", "EXPENSE"));
    }

    @Test
    void analyticsServiceComputesTotalsAndRejectsReversedDateRanges() {
        var service = new AnalyticsService(transactions, accounts, categories);
        var date = LocalDate.of(2026, 11, 20);
        when(transactions.findByOwnerAndReversedFalseAndTransactionDateBetween("alice", date, date)).thenReturn(List.of(
                new FinanceTransaction("alice", 1L, null, "INCOME", new BigDecimal("100.00"), date, null, null),
                new FinanceTransaction("alice", 1L, null, "EXPENSE", new BigDecimal("25.00"), date, null, null)));
        when(accounts.findByOwnerAndActiveTrue("alice")).thenReturn(List.of(new FinancialAccount("alice", "Cash", "CASH", "INR", new BigDecimal("75.00"))));
        var summary = service.summary("alice", date, date);
        assertEquals(new BigDecimal("100.00"), summary.get("income"));
        assertEquals(new BigDecimal("25.00"), summary.get("expenses"));
        assertEquals(new BigDecimal("75.00"), summary.get("netCashFlow"));
        assertThrows(IllegalArgumentException.class, () -> service.expensesByAccount("alice", date.plusDays(1), date));
    }

    @Test
    void notificationServiceMarksAndDeletesOnlyOwnedNotifications() {
        var service = new NotificationService(notifications);
        var notification = new FinanceNotification("alice", "BUDGET", "Near limit", "event");
        when(notifications.findByIdAndOwner(3L, "alice")).thenReturn(Optional.of(notification));
        assertTrue(service.markRead("alice", 3L).read);
        service.delete("alice", 3L);
        verify(notifications).delete(notification);
        when(notifications.findByIdAndOwner(3L, "bob")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.markRead("bob", 3L));
    }

    @Test
    void savingsGoalContributionDebitsAccountAndRecordsLedgerEntry() {
        var accountService = mock(AccountService.class);
        var goal = new SavingsGoal("alice", "Holiday", new BigDecimal("100.00"), null);
        goal.id = 9L;
        var account = new FinancialAccount("alice", "Cash", "CASH", "INR", new BigDecimal("80.00"));
        account.id = 4L;
        when(goals.findByIdAndOwner(9L, "alice")).thenReturn(Optional.of(goal));
        when(accountService.lockOwned("alice", 4L)).thenReturn(account);
        var service = new SavingsGoalService(goals, accountService, transactions, notifications);
        var updated = service.contribute("alice", 9L, 4L, new BigDecimal("30.00"), LocalDate.now());
        assertEquals(new BigDecimal("50.00"), account.balance);
        assertEquals(new BigDecimal("30.00"), updated.currentAmount);
        verify(transactions).save(any(FinanceTransaction.class));
    }

    @Test
    void recurringServiceValidatesFrequencyAndPersistsValidDefinition() {
        var accountService = mock(AccountService.class);
        var categoryService = mock(CategoryService.class);
        when(accountService.get("alice", 1L)).thenReturn(new FinancialAccount("alice", "Cash", "CASH", "INR", BigDecimal.TEN));
        when(recurring.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new RecurringTransactionService(recurring, executions, accountService, categoryService, worker);
        var date = LocalDate.of(2026, 1, 1);
        var created = service.create("alice", 1L, null, "EXPENSE", BigDecimal.ONE, "MONTHLY", date, null, "rent");
        assertEquals(date, created.nextExecutionDate);
        assertThrows(ResponseStatusException.class, () -> service.create("alice", 1L, null, "EXPENSE", BigDecimal.ONE, "HOURLY", date, null, "bad"));
    }

}
