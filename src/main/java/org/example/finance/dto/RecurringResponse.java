package org.example.finance.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record RecurringResponse(Long id,String owner,Long accountId,Long categoryId,String type,BigDecimal amount,String frequency,LocalDate startDate,LocalDate endDate,LocalDate nextExecutionDate,boolean active,boolean paused,String description) {}
