package org.example.finance.dto;
import jakarta.validation.constraints.*;
public record CategoryRequest(@NotBlank @Size(max=80) String name, @NotBlank @Pattern(regexp="INCOME|EXPENSE") String type) {}
