package com.aeroops.incidents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByTenantIdOrderByOpenedAtDesc(String tenantId);

    Optional<Incident> findByIdAndTenantId(UUID id, String tenantId);
}
