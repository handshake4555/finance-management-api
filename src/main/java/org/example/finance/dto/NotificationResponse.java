package org.example.finance.dto;
import java.time.Instant;
public record NotificationResponse(Long id,String owner,String type,String message,boolean read,String eventKey,Instant createdAt) {}
