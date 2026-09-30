package com.aeroops.turnarounds;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TurnaroundRepository extends JpaRepository<Turnaround, UUID> {

    Optional<Turnaround> findByIdAndTenantId(UUID id, String tenantId);

    Optional<Turnaround> findByFlightIdAndTenantId(UUID flightId, String tenantId);
}
