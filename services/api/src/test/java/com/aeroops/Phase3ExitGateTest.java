package com.aeroops;

import com.aeroops.audit.AuditEvent;
import com.aeroops.audit.AuditEventRepository;
import com.aeroops.config.TestJwtDecoderConfig;
import com.aeroops.flights.FlightLeg;
import com.aeroops.flights.FlightLegRepository;
import com.aeroops.ingest.IngestResult;
import com.aeroops.ingest.IngestionService;
import com.aeroops.ingest.InboundEventEnvelopeRequest;
import com.aeroops.ingest.SourceEvent;
import com.aeroops.ingest.SourceEventRepository;
import com.aeroops.stands.AssignStandRequest;
import com.aeroops.stands.StandAssignmentRepository;
import com.aeroops.stands.StandAssignmentResult;
import com.aeroops.stands.StandAssignmentService;
import com.aeroops.tenancy.ActorContext;
import com.aeroops.tenancy.TenantContext;
import com.aeroops.turnarounds.Task;
import com.aeroops.turnarounds.TaskRepository;
import com.aeroops.turnarounds.TaskRevisionRepository;
import com.aeroops.turnarounds.Turnaround;
import com.aeroops.turnarounds.TurnaroundService;
import com.aeroops.turnarounds.TurnaroundTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 3 exit gate from docs/AEROOPS_EXECUTION_PLAN.md Step 3.6: a normal
 * turnaround runs end to end from ingestion through task completion, a duplicate replay
 * of the same event is a true no-op, and a stand conflict is detected, overridden, and
 * shows up correctly on the board with a full audit trail.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class Phase3ExitGateTest {

    private static final String TENANT = "demo-airport";
    private static final String ACTOR = "demo.controller";

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private FlightLegRepository flightLegRepository;

    @Autowired
    private SourceEventRepository sourceEventRepository;

    @Autowired
    private TurnaroundService turnaroundService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskRevisionRepository taskRevisionRepository;

    @Autowired
    private StandAssignmentService standAssignmentService;

    @Autowired
    private StandAssignmentRepository standAssignmentRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID standId;

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

        TenantContext.set(TENANT);
        ActorContext.set(ACTOR);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        ActorContext.clear();
    }

    @Test
    void phase3ExitGate_normalTurnaround_duplicateReplay_andStandConflictOverride() throws Exception {
        // Step A — normal turnaround end-to-end
        InboundEventEnvelopeRequest normalEvent = envelopeFromFixture("fixtures/flight-normal-turnaround.json");

        IngestResult accepted = ingestionService.ingest(normalEvent);
        assertThat(accepted.status()).isEqualTo(SourceEvent.STATUS_ACCEPTED);

        FlightLeg flightA = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATOR", "SIM-FL-1001").orElseThrow();
        Instant updatedAtAfterStepA = flightA.getUpdatedAt();

        Turnaround turnaround = turnaroundService.createForFlight(flightA.getId());
        List<Task> tasks = taskRepository.findByTurnaroundIdAndTenantIdOrderBySequenceAsc(turnaround.getId(), TENANT);
        assertThat(tasks).hasSize(TurnaroundTemplate.STEPS.size());

        for (Task task : tasks) {
            turnaroundService.completeTask(turnaround.getId(), task.getId(), Instant.now());
        }

        List<Task> completedTasks = taskRepository.findByTurnaroundIdAndTenantIdOrderBySequenceAsc(turnaround.getId(), TENANT);
        assertThat(completedTasks).allSatisfy(t -> assertThat(t.getStatus()).isEqualTo(Task.STATUS_COMPLETE));
        for (Task task : completedTasks) {
            assertThat(taskRevisionRepository.findByTaskIdAndTenantIdOrderByRecordedAtAsc(task.getId(), TENANT)).hasSize(1);
        }

        long auditCountAfterStepA = auditEventRepository.findByTenantIdOrderByOccurredAtAsc(TENANT).size();
        long taskCountAfterStepA = completedTasks.size();

        // Step B — duplicate replay is a true no-op
        IngestResult replay = ingestionService.ingest(normalEvent);
        assertThat(replay.status()).isEqualTo(SourceEvent.STATUS_DUPLICATE);

        FlightLeg flightAAfterReplay = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATOR", "SIM-FL-1001").orElseThrow();
        assertThat(flightAAfterReplay.getUpdatedAt()).isEqualTo(updatedAtAfterStepA);
        assertThat(taskRepository.findByTurnaroundIdAndTenantIdOrderBySequenceAsc(turnaround.getId(), TENANT))
                .hasSize((int) taskCountAfterStepA);
        assertThat(auditEventRepository.findByTenantIdOrderByOccurredAtAsc(TENANT)).hasSize((int) auditCountAfterStepA);
        assertThat(sourceEventRepository.findAll()).hasSize(1);

        // Step C — stand conflict, override, resolve
        InboundEventEnvelopeRequest conflictEvent =
                envelopeFromFixture("fixtures/flight-late-inbound-stand-conflict.json");
        IngestResult conflictAccepted = ingestionService.ingest(conflictEvent);
        assertThat(conflictAccepted.status()).isEqualTo(SourceEvent.STATUS_ACCEPTED);

        FlightLeg flightB = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATOR", "SIM-FL-1002").orElseThrow();

        Instant windowStart = Instant.now();
        Instant windowEnd = windowStart.plus(1, ChronoUnit.HOURS);

        StandAssignmentResult firstAssignment = standAssignmentService.assign(
                new AssignStandRequest(flightA.getId(), standId, windowStart, windowEnd, false));
        assertThat(firstAssignment.conflict()).isFalse();

        Instant overlapStart = windowStart.plus(30, ChronoUnit.MINUTES);
        Instant overlapEnd = windowEnd.plus(30, ChronoUnit.MINUTES);

        StandAssignmentResult conflicted = standAssignmentService.assign(
                new AssignStandRequest(flightB.getId(), standId, overlapStart, overlapEnd, false));
        assertThat(conflicted.conflict()).isTrue();
        assertThat(conflicted.conflicting()).extracting(a -> a.getId())
                .containsExactly(firstAssignment.assignment().getId());

        StandAssignmentResult overridden = standAssignmentService.assign(
                new AssignStandRequest(flightB.getId(), standId, overlapStart, overlapEnd, true));
        assertThat(overridden.conflict()).isFalse();
        assertThat(overridden.assignment().isOverride()).isTrue();

        List<com.aeroops.stands.StandAssignment> board =
                standAssignmentRepository.findByTenantIdAndServiceDate(TENANT, flightA.getServiceDate());
        assertThat(board).extracting(a -> a.getId())
                .containsExactlyInAnyOrder(firstAssignment.assignment().getId(), overridden.assignment().getId());

        List<AuditEvent> standAuditEvents = auditEventRepository.findByTenantIdOrderByOccurredAtAsc(TENANT).stream()
                .filter(e -> "STAND_ASSIGNED".equals(e.getAction()))
                .toList();
        assertThat(standAuditEvents).hasSize(2);
        assertThat(standAuditEvents).anySatisfy(e -> assertThat(e.getAfterRef()).contains("OVERRIDE"));
    }

    private InboundEventEnvelopeRequest envelopeFromFixture(String classpathPath) throws Exception {
        JsonNode root = objectMapper.readTree(
                getClass().getClassLoader().getResourceAsStream(classpathPath));
        return new InboundEventEnvelopeRequest(
                root.get("event_id").asText(),
                TENANT,
                root.get("source").asText(),
                root.get("event_type").asText(),
                root.get("schema_version").asText(),
                Instant.parse(root.get("occurred_at").asText()),
                Instant.now(),
                root.get("correlation_id").asText(),
                root.get("payload"));
    }
}
