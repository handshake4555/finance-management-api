package org.example.finance.service;

import org.example.finance.model.RecurringExecution;
import org.example.finance.model.RecurringTransaction;
import org.example.finance.repo.RecurringExecutionRepository;
import org.example.finance.repo.RecurringRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringTransactionService {
    private final RecurringRepository definitions;
    private final RecurringExecutionRepository executions;
    private final AccountService accounts;
    private final CategoryService categories;
    private final RecurringExecutionWorker worker;

    public RecurringTransactionService(RecurringRepository r, RecurringExecutionRepository e, AccountService a, CategoryService c, RecurringExecutionWorker w) {
        definitions = r;
        executions = e;
        accounts = a;
        categories = c;
        worker = w;
    }

    public RecurringTransaction create(String owner, Long accountId, Long categoryId, String type, BigDecimal amount, String frequency, LocalDate start, LocalDate end, String description) {
        if (!List.of("DAILY", "WEEKLY", "MONTHLY", "YEARLY").contains(frequency))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported recurrence frequency");
        if (!List.of("INCOME", "EXPENSE").contains(type) || amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recurring transaction");
        if (start == null || end != null && end.isBefore(start))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recurrence dates");
        accounts.get(owner, accountId);
        if (categoryId != null) {
            var category = categories.get(owner, categoryId);
            if (!category.type.equals(type))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category type mismatch");
        }
        return definitions.save(new RecurringTransaction(owner, accountId, categoryId, type, amount, frequency, start, end, description));
    }

    public List<RecurringTransaction> list(String owner) {
        return definitions.findByOwnerOrderByNextExecutionDateAsc(owner);
    }

    public List<RecurringExecution> history(String owner, Long id) {
        get(owner, id);
        return executions.findByRecurringIdOrderByExecutionDateDesc(id);
    }

    @Transactional
    public RecurringTransaction pause(String owner, Long id, boolean paused) {
        var r = get(owner, id);
        r.paused = paused;
        return r;
    }

    @Transactional
    public void deactivate(String owner, Long id) {
        get(owner, id).active = false;
    }

    private RecurringTransaction get(String owner, Long id) {
        return definitions.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurring transaction not found"));
    }

    @Scheduled(fixedDelayString = "${finance.recurring.fixed-delay-ms:60000}")
    public void processDue() {
        LocalDate today = LocalDate.now();
        for (var r : definitions.findByActiveTrueAndPausedFalseAndNextExecutionDateLessThanEqual(today)) {
            LocalDate due = r.nextExecutionDate;
            int guard = 0;
            while (!due.isAfter(today) && r.active && guard++ < 400) {
                try {
                    worker.execute(r.id, due);
                } catch (RuntimeException ex) {
                    worker.recordFailure(r.id, due, ex.getMessage());
                }
                due = RecurringExecutionWorker.next(r.frequency, due);
                var refresh = definitions.findById(r.id).orElse(r);
                if (!refresh.active) break;
            }
        }
    }
}
