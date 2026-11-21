package org.example.finance.service;

import org.example.finance.model.FinanceTransaction;
import org.example.finance.model.RecurringExecution;
import org.example.finance.model.RecurringTransaction;
import org.example.finance.repo.RecurringExecutionRepository;
import org.example.finance.repo.RecurringRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class RecurringExecutionWorker {
    private final RecurringRepository definitions;
    private final RecurringExecutionRepository executions;
    private final TransactionService transactions;

    public RecurringExecutionWorker(RecurringRepository r, RecurringExecutionRepository e, TransactionService t) {
        definitions = r;
        executions = e;
        transactions = t;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Long id, LocalDate date) {
        if (executions.existsByRecurringIdAndExecutionDate(id, date)) return;
        var r = definitions.findById(id).orElseThrow();
        FinanceTransaction tx = transactions.create(r.owner, r.accountId, r.categoryId, r.type, r.amount, date, r.description, "Recurring definition " + id);
        executions.save(new RecurringExecution(id, date, "SUCCESS", tx.id, null));
        advance(r, date);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long id, LocalDate date, String reason) {
        var r = definitions.findById(id).orElseThrow();
        if (!executions.existsByRecurringIdAndExecutionDate(id, date))
            executions.save(new RecurringExecution(id, date, "FAILED", null, reason == null ? "Processing failed" : reason.substring(0, Math.min(reason.length(), 500))));
        advance(r, date);
    }

    private void advance(RecurringTransaction r, LocalDate date) {
        LocalDate next = next(r.frequency, date);
        if (r.endDate != null && next.isAfter(r.endDate)) r.active = false;
        else r.nextExecutionDate = next;
    }

    static LocalDate next(String frequency, LocalDate date) {
        return switch (frequency) {
            case "DAILY" -> date.plusDays(1);
            case "WEEKLY" -> date.plusWeeks(1);
            case "MONTHLY" -> date.plusMonths(1);
            case "YEARLY" -> date.plusYears(1);
            default -> throw new IllegalArgumentException("Unsupported recurrence frequency");
        };
    }
}
