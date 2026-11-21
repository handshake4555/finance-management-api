package org.example.finance.service;

import org.example.finance.model.FinancialAccount;
import org.example.finance.repo.CategoryRepository;
import org.example.finance.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {
    private TransactionRepository transactions;
    private AccountService accounts;
    private CategoryRepository categories;
    private BudgetService budgets;
    private TransactionService service;

    @BeforeEach
    void setup() {
        transactions = mock(TransactionRepository.class);
        accounts = mock(AccountService.class);
        categories = mock(CategoryRepository.class);
        budgets = mock(BudgetService.class);
        service = new TransactionService(transactions, accounts, categories, budgets);
    }

    @Test
    void expenseDebitsOwnedAccountAndPersistsLedgerEntry() {
        var account = new FinancialAccount("alice", "Cash", "CASH", "INR", new BigDecimal("5000.00"));
        account.id = 4L;
        when(accounts.lockOwned("alice", 4L)).thenReturn(account);
        when(transactions.save(any())).thenAnswer(i -> i.getArgument(0));
        var tx = service.create("alice", 4L, null, "EXPENSE", new BigDecimal("1200.00"), LocalDate.of(2026, 11, 21), "Food", null);
        assertEquals(new BigDecimal("3800.00"), account.balance);
        assertEquals("EXPENSE", tx.type);
        assertEquals("alice", tx.owner);
        verify(budgets).evaluateExpense(tx);
    }

    @Test
    void rejectsExpenseWhenAccountDoesNotHaveFunds() {
        var account = new FinancialAccount("alice", "Cash", "CASH", "INR", new BigDecimal("100.00"));
        account.id = 4L;
        when(accounts.lockOwned("alice", 4L)).thenReturn(account);
        var error = assertThrows(ResponseStatusException.class, () -> service.create("alice", 4L, null, "EXPENSE", new BigDecimal("100.01"), LocalDate.now(), "Overdraw", null));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(new BigDecimal("100.00"), account.balance);
        verify(transactions, never()).save(any());
    }

    @Test
    void rejectsCategoryOwnedBySomeoneElse() {
        var account = new FinancialAccount("alice", "Cash", "CASH", "INR", new BigDecimal("100.00"));
        account.id = 4L;
        when(accounts.lockOwned("alice", 4L)).thenReturn(account);
        when(categories.findByIdAndOwner(8L, "alice")).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.create("alice", 4L, 8L, "EXPENSE", BigDecimal.ONE, LocalDate.now(), null, null));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(transactions, never()).save(any());
    }
}
