package com.adisrivastava.gateway.keys;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_keys")
public class ApiKeys {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "org_id" , nullable = false)
    private UUID orgId;

    @Column(name= "name" , nullable = false)
    private String name;

    @Column(name = "key_hash" , nullable = false , unique = true)
    private String keyHash;

    @Column(name = "prefix" , nullable = false)
    private String prefix;

    // Hibernate always sends an explicit value, so the column defaults in V1 never fire.
    // Every defaulted column is mirrored here instead.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "scopes" , nullable = false , columnDefinition = "text[]")
    private String[] scopes = { "chat" };

    @Column(name = "rate_limit_rpm" , nullable = false)
    private int rateLimitRpm = 60;

    @Column(name = "budget_cents" , nullable = false)
    private long budgetCents = 0;

    @Column(name = "cache_enabled" , nullable = false)
    private boolean cacheEnabled = true;

    // REAL is float4: a double here would fail ddl-auto=validate. Not money, so a
    // floating-point type is the right call — this is a cosine-similarity threshold.
    @Column(name = "cache_threshold" , nullable = false)
    private float cacheThreshold = 0.95f;

    @Column(name="revoked_at")
    private Instant revokedAt;

    @Column(name="created_at" , nullable = false , updatable = false)
    private Instant createdAt = Instant.now();

    protected ApiKeys() {
        // JPA requires a default constructor
    }

    public ApiKeys(UUID orgId , String name , String keyHash , String prefix){
        this.orgId = orgId;
        this.name = name;
        this.keyHash = keyHash;
        this.prefix = prefix;
    }

    /** Active means not revoked. Budget and rate limits are checked elsewhere. */
    public boolean isActive() {
        return revokedAt == null;
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    // Copied on the way in and out: an array handed straight to a caller lets them
    // mutate the entity's state without going through the setter.
    public void setScopes(String[] scopes) {
        this.scopes = scopes.clone();
    }

    public String[] getScopes() {
        return scopes.clone();
    }

    public int getRateLimitRpm() {
        return rateLimitRpm;
    }

    public void setRateLimitRpm(int rateLimitRpm) {
        this.rateLimitRpm = rateLimitRpm;
    }

    public long getBudgetCents() {
        return budgetCents;
    }

    public void setBudgetCents(long budgetCents) {
        this.budgetCents = budgetCents;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public float getCacheThreshold() {
        return cacheThreshold;
    }

    public void setCacheThreshold(float cacheThreshold) {
        this.cacheThreshold = cacheThreshold;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ApiKeys other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // prefix only. keyHash must never reach a log line.
    @Override
    public String toString() {
        return "ApiKeys[id=%s, prefix=%s]".formatted(id, prefix);
    }
}
