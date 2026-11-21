package org.example.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AnnualBudgetRequest(Long categoryId, @Min(2000) int year,
                                  @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal limitAmount) {
}
