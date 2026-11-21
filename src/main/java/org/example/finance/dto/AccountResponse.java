package org.example.finance.dto;
import java.math.BigDecimal;
import java.time.Instant;
public record AccountResponse(Long id,String owner,String name,String type,String currency,BigDecimal balance,boolean active,long version,Instant createdAt) {}
