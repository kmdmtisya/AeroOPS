package com.aeroops.turnarounds;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 3 Step 3.4 exit-gate claim from docs/AEROOPS_EXECUTION_PLAN.md:
 * tasks can be completed out of order, and completing the same task twice appends a
 * second revision rather than overwriting the first — both actors' entries survive.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class TurnaroundServiceTest {

    private static final String TENANT = "demo-airport";
    private static final String ACTOR_ONE = "handler.jane";
    private static final String ACTOR_TWO = "handler.bob";

    @Autowired
    private TurnaroundService turnaroundService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskRevisionRepository taskRevisionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID flightLegId;

    @BeforeEach
    void seedTenantAndBindContext() {
        entityManager.createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, TENANT)
                .executeUpdate();

        flightLegId = UUID.randomUUID();
        entityManager.createNativeQuery(
                "INSERT INTO flight_leg (id, tenant_id, external_id, source_system, carrier, flight_number, " +
                        "service_date, origin, destination, lifecycle, version) " +
                        "VALUES (?1, ?2, 'SA-EXT-TA', 'SIMULATOR', 'SA', 'SA0000', ?3, 'CPT', 'JNB', 'SCHEDULED', 0)")
                .setParameter(1, flightLegId)
                .setParameter(2, TENANT)
                .setParameter(3, LocalDate.now())
                .executeUpdate();

        TenantContext.set(TENANT);
        ActorContext.set(ACTOR_ONE);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        ActorContext.clear();
    }

    @Test
    void completingTasksOutOfOrder_retainsAllRevisions() {
        Turnaround turnaround = turnaroundService.createForFlight(flightLegId);

        List<Task> tasks = taskRepository.findByTurnaroundIdAndTenantIdOrderBySequenceAsc(turnaround.getId(), TENANT);
        assertThat(tasks).hasSize(TurnaroundTemplate.STEPS.size());

        Task taskIndex3 = tasks.get(3);
        Task taskIndex1 = tasks.get(1);

        ActorContext.set(ACTOR_ONE);
        turnaroundService.completeTask(turnaround.getId(), taskIndex3.getId(), Instant.now());

        ActorContext.set(ACTOR_TWO);
        turnaroundService.completeTask(turnaround.getId(), taskIndex1.getId(), Instant.now());

        Task reloadedIndex3 = taskRepository.findByIdAndTurnaroundIdAndTenantId(
                taskIndex3.getId(), turnaround.getId(), TENANT).orElseThrow();
        Task reloadedIndex1 = taskRepository.findByIdAndTurnaroundIdAndTenantId(
                taskIndex1.getId(), turnaround.getId(), TENANT).orElseThrow();

        assertThat(reloadedIndex3.getStatus()).isEqualTo(Task.STATUS_COMPLETE);
        assertThat(reloadedIndex1.getStatus()).isEqualTo(Task.STATUS_COMPLETE);

        List<TaskRevision> revisionsIndex3 = taskRevisionRepository
                .findByTaskIdAndTenantIdOrderByRecordedAtAsc(taskIndex3.getId(), TENANT);
        assertThat(revisionsIndex3).hasSize(1);
        assertThat(revisionsIndex3.get(0).getActor()).isEqualTo(ACTOR_ONE);

        List<TaskRevision> revisionsIndex1 = taskRevisionRepository
                .findByTaskIdAndTenantIdOrderByRecordedAtAsc(taskIndex1.getId(), TENANT);
        assertThat(revisionsIndex1).hasSize(1);
        assertThat(revisionsIndex1.get(0).getActor()).isEqualTo(ACTOR_TWO);

        ActorContext.set(ACTOR_TWO);
        turnaroundService.completeTask(turnaround.getId(), taskIndex3.getId(), Instant.now());

        List<TaskRevision> revisionsIndex3AfterSecondComplete = taskRevisionRepository
                .findByTaskIdAndTenantIdOrderByRecordedAtAsc(taskIndex3.getId(), TENANT);
        assertThat(revisionsIndex3AfterSecondComplete).hasSize(2);
        assertThat(revisionsIndex3AfterSecondComplete.get(0).getActor()).isEqualTo(ACTOR_ONE);
        assertThat(revisionsIndex3AfterSecondComplete.get(1).getActor()).isEqualTo(ACTOR_TWO);
    }
}
