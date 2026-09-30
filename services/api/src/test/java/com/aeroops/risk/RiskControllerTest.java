package com.aeroops.risk;

import com.aeroops.config.TestJwtDecoderConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves GET /v1/flights/{id}/risk is tenant-scoped and correctly wired end-to-end
 * (docs/AEROOPS_EXECUTION_PLAN_PHASE5.md Step 5.1) — the individual rule logic itself is
 * covered without a Spring context by RiskScorerTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class RiskControllerTest {

    private static final String TENANT = "demo-airport";

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID flightLegId;

    @BeforeEach
    void seedTenantAndFlight() {
        entityManager.createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, TENANT)
                .executeUpdate();

        flightLegId = UUID.randomUUID();
        Instant scheduled = Instant.now().minus(1, ChronoUnit.HOURS);
        entityManager.createNativeQuery(
                "INSERT INTO flight_leg (id, tenant_id, external_id, source_system, carrier, flight_number, " +
                        "service_date, origin, destination, lifecycle, scheduled_on_block, actual_on_block, version) " +
                        "VALUES (?1, ?2, 'SA-EXT-RCT', 'SIMULATOR', 'SA', 'SA0002', ?3, 'CPT', 'JNB', 'ON_BLOCK', ?4, ?5, 0)")
                .setParameter(1, flightLegId)
                .setParameter(2, TENANT)
                .setParameter(3, LocalDate.now())
                .setParameter(4, scheduled)
                .setParameter(5, Instant.now())
                .executeUpdate();
    }

    @Test
    void unknownFlight_returnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/flights/{id}/risk", UUID.randomUUID())
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", TENANT))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void knownFlight_returnsRiskAssessment_withHighDelayFlagged() throws Exception {
        mockMvc.perform(get("/v1/flights/{id}/risk", flightLegId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", TENANT))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("HIGH"))
                .andExpect(jsonPath("$.reasons[0].field").value("onBlock"));
    }
}
