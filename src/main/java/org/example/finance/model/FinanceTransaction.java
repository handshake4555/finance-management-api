package org.example.finance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "finance_transactions", indexes = {@Index(name = "idx_tx_owner_date", columnList = "owner,transactionDate"), @Index(name = "idx_tx_account", columnList = "accountId")})
public class FinanceTransaction {
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
    @Column(nullable = false)
    public LocalDate transactionDate;
    @Column(length = 240)
    public String description;
    @Column(length = 1000)
    public String notes;
    @Column(nullable = false)
    public boolean reversed = false;
    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    protected FinanceTransaction() {
    }

    public FinanceTransaction(String owner, Long accountId, Long categoryId, String type, BigDecimal amount, LocalDate date, String description, String notes) {
        this.owner = owner;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.transactionDate = date;
        this.description = description;
        this.notes = notes;
    }
}
