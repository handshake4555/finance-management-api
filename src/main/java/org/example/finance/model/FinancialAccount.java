package org.example.finance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "financial_accounts", indexes = @Index(name = "idx_account_owner", columnList = "owner"))
public class FinancialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String owner;
    @Column(nullable = false, length = 80)
    public String name;
    @Column(nullable = false, length = 24)
    public String type;
    @Column(nullable = false, length = 3)
    public String currency = "INR";
    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal balance = BigDecimal.ZERO;
    @Column(nullable = false)
    public boolean active = true;
    @Version
    public long version;
    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    protected FinancialAccount() {
    }

    public FinancialAccount(String owner, String name, String type, String currency, BigDecimal balance) {
        this.owner = owner;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.balance = balance;
    }
}
