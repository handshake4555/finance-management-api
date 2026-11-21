package org.example.finance.mapper;

import org.example.finance.dto.*;
import org.example.finance.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FinanceMapper {
    private FinanceMapper() {}

    public static AccountResponse toResponse(FinancialAccount value) {
        return new AccountResponse(value.id, value.owner, value.name, value.type, value.currency, value.balance, value.active, value.version, value.createdAt);
    }
    public static CategoryResponse toResponse(Category value) {
        return new CategoryResponse(value.id, value.owner, value.name, value.type, value.active);
    }
    public static TransactionResponse toResponse(FinanceTransaction value) {
        return new TransactionResponse(value.id, value.owner, value.accountId, value.categoryId, value.type, value.amount, value.transactionDate, value.description, value.notes, value.reversed, value.createdAt);
    }
    public static GoalResponse toResponse(SavingsGoal value) {
        BigDecimal remaining = value.targetAmount.subtract(value.currentAmount).max(BigDecimal.ZERO);
        BigDecimal progress = value.currentAmount.multiply(BigDecimal.valueOf(100)).divide(value.targetAmount, 2, RoundingMode.HALF_UP);
        return new GoalResponse(value.id, value.name, value.targetAmount, value.currentAmount, remaining, progress, value.targetDate, value.currentAmount.compareTo(value.targetAmount) >= 0);
    }
    public static RecurringResponse toResponse(RecurringTransaction value) {
        return new RecurringResponse(value.id, value.owner, value.accountId, value.categoryId, value.type, value.amount, value.frequency, value.startDate, value.endDate, value.nextExecutionDate, value.active, value.paused, value.description);
    }
    public static RecurringExecutionResponse toResponse(RecurringExecution value) {
        return new RecurringExecutionResponse(value.id, value.recurringId, value.executionDate, value.status, value.transactionId, value.failureReason, value.processedAt);
    }
    public static NotificationResponse toResponse(FinanceNotification value) {
        return new NotificationResponse(value.id, value.owner, value.type, value.message, value.read, value.eventKey, value.createdAt);
    }
}
