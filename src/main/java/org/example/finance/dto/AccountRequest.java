package org.example.finance.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record AccountRequest(@NotBlank @Size(max=80) String name, @NotBlank String type, @Pattern(regexp="[A-Z]{3}") String currency, @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal openingBalance) {}
