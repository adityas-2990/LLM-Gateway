package com.adisrivastava.gateway.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organizations {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(name = "monthly_budget_cents", nullable = false)
    private long monthlyBudgetCents;

    @Column(name ="created_at" , nullable = false , updatable = false)
    private Instant createdAt = Instant.now();

    protected Organizations() {
        // JPA requires a default constructor
    }

    public Organizations(String name , long monthlyBudgetCents){
        this.name = name;
        this.monthlyBudgetCents = monthlyBudgetCents;
    }

    public UUID getId(){
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMonthlyBudgetCents() {
        return monthlyBudgetCents;
    }

    public void setMonthlyBudgetCents(long monthlyBudgetCents) {
        this.monthlyBudgetCents = monthlyBudgetCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Organizations other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);   
    }

    @Override
    public String toString() {
        return "Organizations[id=%s, name=%s]".formatted(id, name);
    }
}
