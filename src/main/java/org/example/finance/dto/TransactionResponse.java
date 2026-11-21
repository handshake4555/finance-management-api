package org.example.finance.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
public record TransactionResponse(Long id,String owner,Long accountId,Long categoryId,String type,BigDecimal amount,LocalDate transactionDate,String description,String notes,boolean reversed,Instant createdAt) {}
