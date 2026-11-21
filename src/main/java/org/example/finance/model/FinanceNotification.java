package org.example.finance.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "finance_notifications", indexes = @Index(name = "idx_notice_owner_read", columnList = "owner,is_read"))
public class FinanceNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String owner;
    @Column(nullable = false, length = 32)
    public String type;
    @Column(nullable = false, length = 240)
    public String message;
    @Column(name = "is_read", nullable = false)
    public boolean read = false;
    @Column(nullable = false)
    public String eventKey;
    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    protected FinanceNotification() {
    }

    public FinanceNotification(String owner, String type, String message, String eventKey) {
        this.owner = owner;
        this.type = type;
        this.message = message;
        this.eventKey = eventKey;
    }
}
