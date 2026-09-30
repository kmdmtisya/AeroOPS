package com.aeroops.stands;

import com.aeroops.audit.AuditEvent;
import com.aeroops.audit.AuditEventRepository;
import com.aeroops.config.TestJwtDecoderConfig;
import com.aeroops.tenancy.ActorContext;
import com.aeroops.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 3 Step 3.3 exit-gate claim from docs/AEROOPS_EXECUTION_PLAN.md:
 * an overlapping stand assignment is rejected as a conflict (no row created) unless
 * explicitly overridden, and an override writes exactly one AuditEvent.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class StandAssignmentServiceTest {

    private static final String TENANT = "demo-airport";
    private static final String ACTOR = "demo.controller";

    @Autowired
    private StandAssignmentService standAssignmentService;

    @Autowired
    private StandAssignmentRepository standAssignmentRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID standId;
    private UUID flightA;
    private UUID flightB;

    @BeforeEach
    void seedTenantAndBindContext() {
        entityManager.createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, TENANT)
                .executeUpdate();

        standId = UUID.randomUUID();
        entityManager.createNativeQuery(
                "INSERT INTO stand (id, tenant_id, code, terminal) VALUES (?1, ?2, 'A1', 'T1')")
                .setParameter(1, standId)
                .setParameter(2, TENANT)
                .executeUpdate();

        flightA = seedFlight("SA-EXT-A");
        flightB = seedFlight("SA-EXT-B");

        TenantContext.set(TENANT);
        ActorContext.set(ACTOR);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        ActorContext.clear();
    }

    @Test
    void overlappingAssignment_isRejectedUnlessOverridden() {
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);

        StandAssignmentResult first = standAssignmentService.assign(
                new AssignStandRequest(flightA, standId, start, end, false));
        assertThat(first.conflict()).isFalse();

        Instant overlapStart = start.plus(30, ChronoUnit.MINUTES);
        Instant overlapEnd = end.plus(30, ChronoUnit.MINUTES);

        StandAssignmentResult conflicted = standAssignmentService.assign(
                new AssignStandRequest(flightB, standId, overlapStart, overlapEnd, false));
        assertThat(conflicted.conflict()).isTrue();
        assertThat(conflicted.conflicting()).hasSize(1);

        List<StandAssignment> activeAssignments = standAssignmentRepository.findOverlapping(
                TENANT, standId, start, end);
        assertThat(activeAssignments).hasSize(1);

        StandAssignmentResult overridden = standAssignmentService.assign(
                new AssignStandRequest(flightB, standId, overlapStart, overlapEnd, true));
        assertThat(overridden.conflict()).isFalse();
        assertThat(overridden.assignment().isOverride()).isTrue();

        List<AuditEvent> events = auditEventRepository.findByTenantIdOrderByOccurredAtAsc(TENANT);
        assertThat(events).hasSize(2);
        assertThat(events).allSatisfy(e -> assertThat(e.getAction()).isEqualTo("STAND_ASSIGNED"));
        assertThat(events).allSatisfy(e -> assertThat(e.getActor()).isEqualTo(ACTOR));
    }

    private UUID seedFlight(String externalId) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                "INSERT INTO flight_leg (id, tenant_id, external_id, source_system, carrier, flight_number, " +
                        "service_date, origin, destination, lifecycle, version) " +
                        "VALUES (?1, ?2, ?3, 'SIMULATOR', 'SA', 'SA0000', ?4, 'CPT', 'JNB', 'SCHEDULED', 0)")
                .setParameter(1, id)
                .setParameter(2, TENANT)
                .setParameter(3, externalId)
                .setParameter(4, LocalDate.now())
                .executeUpdate();
        return id;
    }
}
