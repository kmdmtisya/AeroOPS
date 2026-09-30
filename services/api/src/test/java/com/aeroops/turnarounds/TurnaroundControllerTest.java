package com.aeroops.turnarounds;

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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the flight-scoped turnaround lookup (GET /v1/flights/{id}/turnarounds) added for
 * the AOCC dashboard's flight detail page (docs/AEROOPS_EXECUTION_PLAN_PHASE4.md Step 4.3),
 * since no prior endpoint let a caller resolve a turnaround from a flight id alone.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class TurnaroundControllerTest {

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
        entityManager.createNativeQuery(
                "INSERT INTO flight_leg (id, tenant_id, external_id, source_system, carrier, flight_number, " +
                        "service_date, origin, destination, lifecycle, version) " +
                        "VALUES (?1, ?2, 'SA-EXT-TCT', 'SIMULATOR', 'SA', 'SA0001', ?3, 'CPT', 'JNB', 'SCHEDULED', 0)")
                .setParameter(1, flightLegId)
                .setParameter(2, TENANT)
                .setParameter(3, LocalDate.now())
                .executeUpdate();
    }

    @Test
    void noTurnaroundYet_returnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/flights/{id}/turnarounds", flightLegId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", TENANT))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void afterCreate_returnsTheTurnaroundForThatFlight() throws Exception {
        mockMvc.perform(post("/v1/flights/{id}/turnarounds", flightLegId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", TENANT)
                                        .claim("preferred_username", "demo.controller"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CONTROLLER"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/flights/{id}/turnarounds", flightLegId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", TENANT))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightLegId").value(flightLegId.toString()));
    }
}
