package org.example.finance.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TransactionRequest(@NotNull Long accountId, Long categoryId, @NotBlank @Pattern(regexp="INCOME|EXPENSE") String type, @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount, @NotNull LocalDate transactionDate, @Size(max=240) String description, @Size(max=1000) String notes) {}
