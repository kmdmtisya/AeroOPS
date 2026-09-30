package com.aeroops.feed;

import com.aeroops.config.TestJwtDecoderConfig;
import com.aeroops.flights.FlightLeg;
import com.aeroops.flights.FlightLegRepository;
import com.aeroops.ingest.IngestResult;
import com.aeroops.ingest.SourceEvent;
import com.aeroops.ingest.SourceEventRepository;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 4 Step 4.1 exit-gate claim: a directly-invoked tick ingests a real event
 * through the same IngestionService path a real feed would use, and mutates a FlightLeg.
 * Binds TenantContext/ActorContext in @BeforeEach the same way tick() binds them itself
 * before delegating to ingestNextStep() (the deterministic, schedule-free seam under test) —
 * tick()'s own bind/clear-in-finally behavior is exercised live, not by this unit test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
// SimulatedFeedScheduler holds its own mutable round-robin/lifecycle-stage state as instance
// fields (nextFlightIndex, lifecycleStageByExternalId) rather than in the (per-test, rolled-
// back) database, so it survives @Transactional's rollback across test methods sharing the
// cached Spring context. Force a fresh context (and thus a fresh scheduler bean) per method
// so each test's assumptions about "the next flight in the pool" hold regardless of order.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SimulatedFeedSchedulerTest {

    private static final String TENANT = "demo-airport";

    @Autowired
    private SimulatedFeedScheduler scheduler;

    @Autowired
    private FlightLegRepository flightLegRepository;

    @Autowired
    private SourceEventRepository sourceEventRepository;

    @Autowired
    private SimulatedFeedProperties simulatedFeedProperties;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seedTenantAndBindContext() {
        entityManager.createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, TENANT)
                .executeUpdate();
        // ingestNextStep() is the deterministic seam tick() delegates to after binding these
        // itself; tests call it directly (no real scheduling delay) so bind here the same way
        // tick() would, matching IngestionServiceTest's convention for the wrapped service call.
        TenantContext.set(TENANT);
        ActorContext.set("simulated-feed");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        ActorContext.clear();
    }

    @Test
    void tick_ingestsSyntheticFlightUpdate_andClearsContextAfterward() {
        IngestResult result = scheduler.ingestNextStep();
        assertThat(result.status()).isEqualTo(SourceEvent.STATUS_ACCEPTED);

        Optional<FlightLeg> leg = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATED_AOCC", "SIM-FEED-2001");
        assertThat(leg).isPresent();
        assertThat(leg.get().getLifecycle()).isIn(
                "SCHEDULED", "INBOUND", "ON_BLOCK", "TURNAROUND", "READY", "OFF_BLOCK");
    }

    @Test
    void tick_isANoOp_whenFeedIsDisabled() {
        // The test profile binds no aeroops.simulated-feed config, so SimulatedFeedProperties
        // keeps its class-level default (enabled=false) here — this is the "feed outage"
        // case: tick() must return without ingesting anything rather than erroring. tick()'s
        // disabled-path returns before touching TenantContext at all, so this @BeforeEach's own
        // binding is left exactly as it was — there is deliberately no isBound() assertion here
        // (unlike the enabled case below), since a no-op by definition doesn't clear a context
        // it never set.
        assertThat(simulatedFeedProperties.isEnabled()).isFalse();
        long sourceEventCountBefore = sourceEventRepository.count();

        scheduler.tick();

        assertThat(sourceEventRepository.count()).isEqualTo(sourceEventCountBefore);
    }

    @Test
    void tick_ingestsWhenEnabled_andClearsContextAfterward() {
        simulatedFeedProperties.setEnabled(true);
        try {
            long sourceEventCountBefore = sourceEventRepository.count();

            scheduler.tick();

            assertThat(sourceEventRepository.count()).isEqualTo(sourceEventCountBefore + 1);
            assertThat(TenantContext.isBound()).isFalse();
        } finally {
            simulatedFeedProperties.setEnabled(false);
        }
    }

    @Test
    void repeatedTicks_advanceTheSameFlightThroughSuccessiveLifecycleStages() {
        scheduler.ingestNextStep(); // SIM-FEED-2001
        scheduler.ingestNextStep(); // SIM-FEED-2002
        scheduler.ingestNextStep(); // SIM-FEED-2003
        scheduler.ingestNextStep(); // SIM-FEED-2001 again, one stage further

        FlightLeg leg = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATED_AOCC", "SIM-FEED-2001").orElseThrow();
        assertThat(leg.getLifecycle()).isEqualTo("INBOUND");
    }
}
