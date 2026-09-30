package com.aeroops.feed;

import com.aeroops.config.TestJwtDecoderConfig;
import com.aeroops.flights.FlightLeg;
import com.aeroops.flights.FlightLegRepository;
import com.aeroops.incidents.CreateIncidentRequest;
import com.aeroops.ingest.IngestResult;
import com.aeroops.ingest.SourceEvent;
import com.aeroops.tenancy.ActorContext;
import com.aeroops.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the Phase 4 exit gate from docs/AEROOPS_EXECUTION_PLAN_PHASE4.md Step 4.4: a
 * directly-invoked simulated-feed tick (4.1) produces a real, visible flight mutation, and
 * role authorization (4.2) still holds on the same tenant/request path a dashboard would use.
 * Lives in com.aeroops.feed (not com.aeroops, unlike Phase3ExitGateTest) because it needs
 * package-visible access to SimulatedFeedScheduler.ingestNextStep(), the deterministic seam
 * SimulatedFeedSchedulerTest also uses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class Phase4ExitGateTest {

    private static final String TENANT = "demo-airport";

    @Autowired
    private SimulatedFeedScheduler scheduler;

    @Autowired
    private FlightLegRepository flightLegRepository;

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedTenantAndBindContext() {
        entityManager.createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, TENANT)
                .executeUpdate();
        TenantContext.set(TENANT);
        ActorContext.set("simulated-feed");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        ActorContext.clear();
    }

    @Test
    void phase4ExitGate_simulatedFeedTickIsVisible_andRoleAuthorizationHolds() throws Exception {
        // 4.1 — a directly-invoked tick produces a real, visible flight mutation.
        IngestResult firstTick = scheduler.ingestNextStep();
        assertThat(firstTick.status()).isEqualTo(SourceEvent.STATUS_ACCEPTED);

        Optional<FlightLeg> afterFirstTick = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATED_AOCC", "SIM-FEED-2001");
        assertThat(afterFirstTick).isPresent();
        Instant updatedAtAfterFirstTick = afterFirstTick.get().getUpdatedAt();

        // A second full pass round the 3-flight pool advances SIM-FEED-2001 one more lifecycle
        // stage and moves its updatedAt forward again — the visible "live dashboard" proof.
        scheduler.ingestNextStep(); // SIM-FEED-2002
        scheduler.ingestNextStep(); // SIM-FEED-2003
        IngestResult secondTickForSameFlight = scheduler.ingestNextStep(); // SIM-FEED-2001 again
        assertThat(secondTickForSameFlight.status()).isEqualTo(SourceEvent.STATUS_ACCEPTED);

        FlightLeg afterSecondTick = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATED_AOCC", "SIM-FEED-2001").orElseThrow();
        assertThat(afterSecondTick.getUpdatedAt()).isAfter(updatedAtAfterFirstTick);
        assertThat(afterSecondTick.getLifecycle()).isEqualTo("INBOUND");

        // Visible via the same read path the dashboard's /flights list uses.
        mockMvc.perform(get("/v1/flights")
                        .with(jwt()
                                .jwt(builder -> builder.claim("tenant_id", TENANT).claim("preferred_username", "demo.viewer"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk());

        // 4.2 — a viewer-role token on the same tenant is blocked from a mutating endpoint...
        mockMvc.perform(post("/v1/incidents")
                        .with(jwt()
                                .jwt(builder -> builder.claim("tenant_id", TENANT).claim("preferred_username", "demo.viewer"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateIncidentRequest("BAGGAGE_DELAY", "Late bags (exit gate)", null, null))))
                .andExpect(status().isForbidden());

        // ...while a controller-role token on the same tenant succeeds on the identical request.
        mockMvc.perform(post("/v1/incidents")
                        .with(jwt()
                                .jwt(builder -> builder.claim("tenant_id", TENANT).claim("preferred_username", "demo.controller"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CONTROLLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateIncidentRequest("BAGGAGE_DELAY", "Late bags (exit gate)", null, null))))
                .andExpect(status().isCreated());
    }
}
