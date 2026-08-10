package com.adisrivastava.gateway.keys;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKeys, UUID> {
    Optional<ApiKeys> findByKeyHashAndRevokedAtIsNull(String keyHash);

}
