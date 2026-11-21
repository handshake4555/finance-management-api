package org.example.finance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "finance_categories", indexes = @Index(name = "idx_category_owner", columnList = "owner"))
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String owner;
    @Column(nullable = false, length = 80)
    public String name;
    @Column(nullable = false, length = 12)
    public String type;
    @Column(nullable = false)
    public boolean active = true;

    protected Category() {
    }

    public Category(String owner, String name, String type) {
        this.owner = owner;
        this.name = name;
        this.type = type;
    }
}
