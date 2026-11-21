package org.example.finance.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_executions", uniqueConstraints = @UniqueConstraint(name = "uk_recurring_execution_date", columnNames = {"recurring_id", "execution_date"}))
public class RecurringExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "recurring_id", nullable = false)
    public Long recurringId;
    @Column(name = "execution_date", nullable = false)
    public LocalDate executionDate;
    @Column(nullable = false, length = 16)
    public String status;
    public Long transactionId;
    @Column(length = 500)
    public String failureReason;
    @Column(nullable = false)
    public Instant processedAt = Instant.now();

    protected RecurringExecution() {
    }

    public RecurringExecution(Long recurringId, LocalDate date, String status, Long transactionId, String reason) {
        this.recurringId = recurringId;
        this.executionDate = date;
        this.status = status;
        this.transactionId = transactionId;
        this.failureReason = reason;
    }
}
