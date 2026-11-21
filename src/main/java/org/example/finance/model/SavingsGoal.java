package org.example.finance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals", indexes = @Index(name = "idx_goal_owner", columnList = "owner"))
public class SavingsGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String owner;
    @Column(nullable = false, length = 100)
    public String name;
    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal targetAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal currentAmount = BigDecimal.ZERO;
    public LocalDate targetDate;
    @Column(nullable = false)
    public boolean active = true;

    protected SavingsGoal() {
    }

    public SavingsGoal(String owner, String name, BigDecimal target, LocalDate date) {
        this.owner = owner;
        this.name = name;
        this.targetAmount = target;
        this.targetDate = date;
    }
}
