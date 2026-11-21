package org.example.finance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "finance_budgets", indexes = @Index(name = "idx_budget_owner", columnList = "owner"))
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String owner;
    public Long categoryId;
    @Column(name = "budget_year", nullable = false)
    public int year;
    @Column(name = "budget_month", nullable = false)
    public int month;
    @Column(nullable = false, length = 12)
    public String period = "MONTHLY";
    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal limitAmount;
    @Column(nullable = false)
    public boolean active = true;
    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    protected Budget() {
    }

    public Budget(String owner, Long categoryId, int year, int month, BigDecimal limit) {
        this.owner = owner;
        this.categoryId = categoryId;
        this.year = year;
        this.month = month;
        this.limitAmount = limit;
    }

    public Budget(String owner, Long categoryId, int year, int month, BigDecimal limit, String period) {
        this(owner, categoryId, year, month, limit);
        this.period = period;
    }
}
