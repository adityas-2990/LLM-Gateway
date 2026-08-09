package com.adisrivastava.gateway.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class Users {
    

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "org_id" , nullable = false)
    private UUID orgId;

    @Column(name = "email" , nullable = false , unique = true)
    private String email;

    @Column(name = "password_hash" , nullable = false)
    private String passwordHash;

    @Column(name = "role" , nullable = false)
    private String role;

    @Column(name = "created_at" , nullable = false , updatable = false)
    private Instant createdAt = Instant.now();

    protected Users() {
        // JPA requires a default constructor
    }

    public Users(UUID orgId , String email , String passwordHash , String role){
        this.orgId = orgId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public UUID getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Users other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Users[id=%s, email=%s]".formatted(id, email);
    }
}
