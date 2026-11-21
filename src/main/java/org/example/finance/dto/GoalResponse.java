package org.example.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalResponse(Long id, String name, BigDecimal targetAmount, BigDecimal currentAmount,
                           BigDecimal remaining, BigDecimal progressPercent, LocalDate targetDate, boolean completed) {
}
