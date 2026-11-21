package org.example.finance.dto;
import java.time.Instant;
import java.time.LocalDate;
public record RecurringExecutionResponse(Long id,Long recurringId,LocalDate executionDate,String status,Long transactionId,String failureReason,Instant processedAt) {}
