package org.example.finance.service;

import org.example.finance.model.FinanceTransaction;
import org.example.finance.model.RecurringTransaction;
import org.example.finance.repo.RecurringExecutionRepository;
import org.example.finance.repo.RecurringRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RecurringExecutionWorkerTest {
    @Test
    void executesDueDefinitionOnceAndAdvancesMonthlyDate() {
        var definitions = mock(RecurringRepository.class);
        var executions = mock(RecurringExecutionRepository.class);
        var txService = mock(TransactionService.class);
        var worker = new RecurringExecutionWorker(definitions, executions, txService);
        var recurring = new RecurringTransaction("u", 1L, null, "INCOME", new BigDecimal("100.00"), "MONTHLY", LocalDate.of(2026, 1, 31), null, "Salary");
        recurring.id = 7L;
        var tx = new FinanceTransaction("u", 1L, null, "INCOME", new BigDecimal("100.00"), LocalDate.of(2026, 1, 31), "Salary", null);
        tx.id = 19L;
        when(definitions.findById(7L)).thenReturn(Optional.of(recurring));
        when(executions.existsByRecurringIdAndExecutionDate(7L, LocalDate.of(2026, 1, 31))).thenReturn(false);
        when(txService.create("u", 1L, null, "INCOME", new BigDecimal("100.00"), LocalDate.of(2026, 1, 31), "Salary", "Recurring definition 7")).thenReturn(tx);
        worker.execute(7L, LocalDate.of(2026, 1, 31));
        assertEquals(LocalDate.of(2026, 2, 28), recurring.nextExecutionDate);
        verify(executions).save(argThat(e -> e.status.equals("SUCCESS") && e.transactionId.equals(19L)));
    }

    @Test
    void doesNotExecuteDuplicateOccurrence() {
        var definitions = mock(RecurringRepository.class);
        var executions = mock(RecurringExecutionRepository.class);
        var txService = mock(TransactionService.class);
        var worker = new RecurringExecutionWorker(definitions, executions, txService);
        when(executions.existsByRecurringIdAndExecutionDate(2L, LocalDate.of(2026, 1, 1))).thenReturn(true);
        worker.execute(2L, LocalDate.of(2026, 1, 1));
        verifyNoInteractions(definitions, txService);
        verify(executions, never()).save(any());
    }
}
