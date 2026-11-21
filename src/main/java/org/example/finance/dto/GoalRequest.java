package org.example.finance.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record GoalRequest(@NotBlank @Size(max=100) String name, @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal targetAmount, LocalDate targetDate) {}
