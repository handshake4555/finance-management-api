package org.example.finance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions", indexes = @Index(name = "idx_recurring_due", columnList = "active,paused,next_execution_date"))
public class RecurringTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String owner;
    @Column(nullable = false)
    public Long accountId;
    public Long categoryId;
    @Column(nullable = false, length = 12)
    public String type;
    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;
    @Column(nullable = false, length = 12)
    public String frequency;
    @Column(nullable = false)
    public LocalDate startDate;
    public LocalDate endDate;
    @Column(nullable = false)
    public LocalDate nextExecutionDate;
    @Column(nullable = false)
    public boolean active = true;
    @Column(nullable = false)
    public boolean paused = false;
    @Column(length = 240)
    public String description;

    protected RecurringTransaction() {
    }

    public RecurringTransaction(String owner, Long accountId, Long categoryId, String type, BigDecimal amount, String frequency, LocalDate startDate, LocalDate endDate, String description) {
        this.owner = owner;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.nextExecutionDate = startDate;
        this.description = description;
    }
}
