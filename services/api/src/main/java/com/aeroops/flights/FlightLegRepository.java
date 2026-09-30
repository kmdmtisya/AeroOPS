package com.aeroops.flights;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlightLegRepository extends JpaRepository<FlightLeg, UUID> {

    List<FlightLeg> findByTenantIdOrderByScheduledOnBlockAsc(String tenantId);

    Optional<FlightLeg> findByIdAndTenantId(UUID id, String tenantId);

    Optional<FlightLeg> findByTenantIdAndSourceSystemAndExternalId(
            String tenantId, String sourceSystem, String externalId);
}
