package org.example.finance.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BudgetRequest(Long categoryId, @Min(2000) int year, @Min(1) @Max(12) int month,
                            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal limitAmount) {
}
