package org.example.finance.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ContributionRequest(@NotNull Long accountId, @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount, @NotNull LocalDate date) {}
