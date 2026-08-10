package com.adisrivastava.gateway.keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.adisrivastava.gateway.TestcontainersConfiguration;
import com.adisrivastava.gateway.auth.OrganizationRepository;
import com.adisrivastava.gateway.auth.Organizations;

/**
 * Deliberately not @Transactional, for the same reason as UserRepositoryTest: every save
 * has to really commit so the unique constraint fires against Postgres, and so the finders
 * read through a fresh persistence context instead of handing back the instance already in
 * the first-level cache. That matters most for the scopes array — an in-memory hit would
 * prove nothing about the text[] mapping.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApiKeyRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Organizations org;

    @BeforeEach
    void clean() {
        // Only api_keys is emptied. Organizations are left alone because users in another
        // test class may still hold FKs into them, and stale orgs are inert here anyway.
        apiKeyRepository.deleteAll();
        org = organizationRepository.save(new Organizations("Acme", 50_000));
    }

    @Test
    void savesAKeyAndReadsItBackWithEveryScopeIntact() {
        ApiKeys key = new ApiKeys(org.getId(), "ci-pipeline", "hash-round-trip", "gw_live_a3f2");
        key.setScopes(new String[] { "chat", "embeddings", "admin" });
        key.setRateLimitRpm(120);
        key.setBudgetCents(25_000);
        key.setCacheEnabled(false);
        key.setCacheThreshold(0.87f);

        ApiKeys saved = apiKeyRepository.save(key);

        Optional<ApiKeys> found = apiKeyRepository.findByKeyHashAndRevokedAtIsNull("hash-round-trip");

        assertThat(found).isPresent();
        ApiKeys read = found.get();
        assertThat(read).isEqualTo(saved);
        assertThat(read.getScopes()).containsExactly("chat", "embeddings", "admin");
        assertThat(read.getOrgId()).isEqualTo(org.getId());
        assertThat(read.getName()).isEqualTo("ci-pipeline");
        assertThat(read.getPrefix()).isEqualTo("gw_live_a3f2");
        assertThat(read.getRateLimitRpm()).isEqualTo(120);
        assertThat(read.getBudgetCents()).isEqualTo(25_000L);
        assertThat(read.isCacheEnabled()).isFalse();
        assertThat(read.getCacheThreshold()).isEqualTo(0.87f);
        assertThat(read.getRevokedAt()).isNull();
        assertThat(read.isActive()).isTrue();
        assertThat(read.getCreatedAt()).isNotNull();
    }

    @Test
    void aKeyBuiltFromTheConstructorAloneMatchesTheColumnDefaults() {
        // Hibernate writes every column explicitly, so V1's DEFAULTs never fire. These
        // values come from the entity's field initialisers and have to agree with V1.
        apiKeyRepository.save(new ApiKeys(org.getId(), "defaults", "hash-defaults", "gw_live_dflt"));

        ApiKeys read = apiKeyRepository.findByKeyHashAndRevokedAtIsNull("hash-defaults").orElseThrow();

        assertThat(read.getScopes()).containsExactly("chat");
        assertThat(read.getRateLimitRpm()).isEqualTo(60);
        assertThat(read.getBudgetCents()).isZero();
        assertThat(read.isCacheEnabled()).isTrue();
        assertThat(read.getCacheThreshold()).isEqualTo(0.95f);
    }

    @Test
    void revokingAKeyHidesItFromTheLookup() {
        ApiKeys key = apiKeyRepository.save(
                new ApiKeys(org.getId(), "to-revoke", "hash-revoked", "gw_live_revk"));

        assertThat(apiKeyRepository.findByKeyHashAndRevokedAtIsNull("hash-revoked")).isPresent();

        key.revoke();
        apiKeyRepository.saveAndFlush(key);

        assertThat(apiKeyRepository.findByKeyHashAndRevokedAtIsNull("hash-revoked")).isEmpty();

        // The row is still there — revocation is a tombstone, not a delete.
        ApiKeys revoked = apiKeyRepository.findById(key.getId()).orElseThrow();
        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(revoked.isActive()).isFalse();
    }

    @Test
    void findByKeyHashAndRevokedAtIsNullReturnsEmptyForAnUnknownHash() {
        assertThat(apiKeyRepository.findByKeyHashAndRevokedAtIsNull("no-such-hash")).isEmpty();
    }

    @Test
    void twoKeysWithTheSameHashViolateTheUniqueConstraint() {
        apiKeyRepository.saveAndFlush(new ApiKeys(org.getId(), "first", "hash-dup", "gw_live_one"));

        ApiKeys duplicate = new ApiKeys(org.getId(), "second", "hash-dup", "gw_live_two");

        // saveAndFlush, not save: the INSERT has to reach Postgres inside the assertion
        // rather than at transaction commit, which is where the constraint actually lives.
        assertThatThrownBy(() -> apiKeyRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theLookupIsServedByThePartialIndexOnKeyHash() {
        apiKeyRepository.save(new ApiKeys(org.getId(), "probe", "hash-probe", "gw_live_prb"));

        String plan = explain(
                "SELECT * FROM api_keys WHERE key_hash = 'hash-probe' AND revoked_at IS NULL");

        assertThat(plan).contains("Index Scan using idx_api_keys_hash");
    }

    @Test
    void droppingTheRevokedAtPredicateLosesThePartialIndex() {
        // Why findByKeyHash alone would be the wrong method: idx_api_keys_hash is declared
        // WHERE revoked_at IS NULL, and Postgres will only use a partial index when the
        // query's predicate implies the index's. Without it the planner falls back to the
        // UNIQUE constraint's index, which has to visit revoked rows too.
        apiKeyRepository.save(new ApiKeys(org.getId(), "probe", "hash-probe", "gw_live_prb"));

        String plan = explain("SELECT * FROM api_keys WHERE key_hash = 'hash-probe'");

        assertThat(plan).doesNotContain("idx_api_keys_hash");
        assertThat(plan).contains("Index Scan using api_keys_key_hash_key");
    }

    /**
     * Runs EXPLAIN with sequential scans penalised. On a table this small Postgres would
     * correctly prefer a seq scan over any index, so without that the plan says nothing
     * about whether the index is usable — which is the question the story actually asks.
     */
    private String explain(String sql) {
        return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET enable_seqscan = off");
                StringBuilder plan = new StringBuilder();
                try (ResultSet rows = statement.executeQuery("EXPLAIN " + sql)) {
                    while (rows.next()) {
                        plan.append(rows.getString(1)).append('\n');
                    }
                }
                return plan.toString();
            } finally {
                // The connection goes back to the pool: leave the planner as we found it.
                try (Statement reset = connection.createStatement()) {
                    reset.execute("RESET enable_seqscan");
                }
            }
        });
    }
}
