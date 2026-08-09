package com.adisrivastava.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.adisrivastava.gateway.TestcontainersConfiguration;

/**
 * Deliberately not @Transactional. Every save has to really commit so that the unique
 * constraint fires against Postgres, and so findByEmail reads through a fresh persistence
 * context instead of handing back the instance already in the first-level cache.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();          // users first: org_id is an FK to organizations
        organizationRepository.deleteAll();
    }

    @Test
    void savesAnOrgThenAUserInItAndReadsBackByEmail() {
        Organizations org = organizationRepository.save(new Organizations("Acme", 50_000));

        Users saved = userRepository.save(
                new Users(org.getId(), "admin@acme.test", "hash", "ADMIN"));

        Optional<Users> found = userRepository.findByEmail("admin@acme.test");

        assertThat(found).isPresent();
        Users user = found.get();
        assertThat(user).isEqualTo(saved);
        assertThat(user.getOrgId()).isEqualTo(org.getId());
        assertThat(user.getEmail()).isEqualTo("admin@acme.test");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo("ADMIN");
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void savingTwoUsersWithTheSameEmailViolatesTheUniqueConstraint() {
        Organizations org = organizationRepository.save(new Organizations("Acme", 0));

        userRepository.saveAndFlush(new Users(org.getId(), "dup@acme.test", "hash", "ADMIN"));

        Users duplicate = new Users(org.getId(), "dup@acme.test", "other-hash", "ADMIN");

        // saveAndFlush, not save: the INSERT has to reach Postgres inside the assertion
        // rather than at transaction commit, which is where the constraint actually lives.
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByEmailReturnsEmptyForAnUnknownAddress() {
        assertThat(userRepository.findByEmail("nobody@acme.test")).isEmpty();
    }

    @Test
    void budgetRoundTripsAsWholeCents() {
        // 2_500_000 cents = $25,000. Exact through a BIGINT column and back.
        Organizations saved = organizationRepository.save(new Organizations("Acme", 2_500_000));

        Optional<Organizations> found = organizationRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMonthlyBudgetCents()).isEqualTo(2_500_000L);
        assertThat(found.get().getName()).isEqualTo("Acme");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }
}
