package org.example.finance.dto;

import java.math.BigDecimal;

public record BudgetResponse(Long id, Long categoryId, int year, int month, String period,
                             BigDecimal limit, BigDecimal spent, BigDecimal remaining,
                             BigDecimal utilizationPercent, boolean overspent) {
}
