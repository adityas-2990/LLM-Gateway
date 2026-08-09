package com.adisrivastava.gateway.auth;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organizations , UUID> {
}
