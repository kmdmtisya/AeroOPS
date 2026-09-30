package com.aeroops.stands;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StandAssignmentRepository extends JpaRepository<StandAssignment, UUID> {

    /**
     * Range-overlap check for one stand's active assignments — plain SQL, not a scheduling
     * algorithm, per the roadmap's "no auto-assignment" rule (assignment is always
     * human-initiated; this only detects conflicts for the caller to accept or override).
     */
    @Query(value = "SELECT * FROM stand_assignment WHERE tenant_id = :tenantId AND stand_id = :standId " +
            "AND status = 'ACTIVE' AND window_start < :windowEnd AND window_end > :windowStart",
            nativeQuery = true)
    List<StandAssignment> findOverlapping(@Param("tenantId") String tenantId, @Param("standId") UUID standId,
                                           @Param("windowStart") Instant windowStart, @Param("windowEnd") Instant windowEnd);

    @Query(value = "SELECT sa.* FROM stand_assignment sa JOIN flight_leg fl ON fl.id = sa.flight_id " +
            "WHERE sa.tenant_id = :tenantId AND fl.service_date = :serviceDate AND sa.status = 'ACTIVE'",
            nativeQuery = true)
    List<StandAssignment> findByTenantIdAndServiceDate(@Param("tenantId") String tenantId,
                                                         @Param("serviceDate") LocalDate serviceDate);
}
