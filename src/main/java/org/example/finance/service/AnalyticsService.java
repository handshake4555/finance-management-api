package org.example.finance.service;

import org.example.finance.model.FinanceTransaction;
import org.example.finance.repo.AccountRepository;
import org.example.finance.repo.CategoryRepository;
import org.example.finance.repo.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final TransactionRepository transactions;
    private final AccountRepository accounts;
    private final CategoryRepository categories;

    public AnalyticsService(TransactionRepository t, AccountRepository a, CategoryRepository c) {
        transactions = t;
        accounts = a;
        categories = c;
    }

    public Map<String, Object> summary(String owner, LocalDate from, LocalDate to) {
        var txs = range(owner, from, to);
        BigDecimal income = sum(txs, "INCOME"), expense = sum(txs, "EXPENSE"), balance = accounts.findByOwnerAndActiveTrue(owner).stream().map(a -> a.balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("income", income, "expenses", expense, "netCashFlow", income.subtract(expense), "balances", balance, "savingsRate", income.signum() == 0 ? BigDecimal.ZERO : income.subtract(expense).multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP), "recentTransactions", txs.stream().sorted(Comparator.comparing((FinanceTransaction t) -> t.transactionDate).reversed()).limit(10).toList());
    }

    public Map<String, BigDecimal> expensesByCategory(String owner, LocalDate from, LocalDate to) {
        Map<Long, String> names = categories.findByOwnerAndActiveTrue(owner).stream().collect(Collectors.toMap(c -> c.id, c -> c.name));
        return range(owner, from, to).stream().filter(t -> t.type.equals("EXPENSE")).collect(Collectors.groupingBy(t -> names.getOrDefault(t.categoryId, "Uncategorized"), Collectors.reducing(BigDecimal.ZERO, t -> t.amount, BigDecimal::add)));
    }

    public Map<String, BigDecimal> monthlyCashFlow(String owner, int year) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            LocalDate start = LocalDate.of(year, month, 1), end = start.withDayOfMonth(start.lengthOfMonth());
            List<FinanceTransaction> txs = range(owner, start, end);
            result.put(String.format("%04d-%02d-income", year, month), sum(txs, "INCOME"));
            result.put(String.format("%04d-%02d-expense", year, month), sum(txs, "EXPENSE"));
        }
        return result;
    }

    public Map<String, Object> monthlyReport(String owner, int year, int month) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("month must be 1 through 12");
        LocalDate start = LocalDate.of(year, month, 1);
        return summary(owner, start, start.withDayOfMonth(start.lengthOfMonth()));
    }

    public Map<String, Object> yearlyReport(String owner, int year) {
        return summary(owner, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public Map<Long, BigDecimal> expensesByAccount(String owner, LocalDate from, LocalDate to) {
        return range(owner, from, to).stream().filter(t -> t.type.equals("EXPENSE")).collect(Collectors.groupingBy(t -> t.accountId, Collectors.reducing(BigDecimal.ZERO, t -> t.amount, BigDecimal::add)));
    }

    private List<FinanceTransaction> range(String owner, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("from must not be after to");
        return transactions.findByOwnerAndReversedFalseAndTransactionDateBetween(owner, from, to);
    }

    private BigDecimal sum(List<FinanceTransaction> txs, String type) {
        return txs.stream().filter(t -> t.type.equals(type)).map(t -> t.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
