package com.aeroops;

import com.aeroops.config.TestJwtDecoderConfig;
import com.aeroops.incidents.Incident;
import com.aeroops.incidents.IncidentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the Phase 5 Step 5.3 exit-gate claim (roadmap step 5.6): tenant A's token must
 * never read or mutate tenant B's real resources, at the full HTTP path — not just the
 * repository-level scoping already proven by FlightLegRepositoryTenantIsolationTest and
 * IncidentRepositoryTenantIsolationTest. Seeds two real tenants and issues every request
 * as tenant A against tenant B's actual resource ids, asserting 404 rather than a leak or
 * an unmapped-exception 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class CrossTenantAuthorizationTest {

    private static final String TENANT_A = "cross-tenant-a";
    private static final String TENANT_B = "cross-tenant-b";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID tenantBFlightId;
    private UUID tenantBIncidentId;

    @BeforeEach
    void seedTwoTenantsWithResourcesOwnedByTenantB() {
        seedTenant(TENANT_A);
        seedTenant(TENANT_B);

        tenantBFlightId = seedFlight(TENANT_B, "XT-EXT-1");
        tenantBIncidentId = seedIncident(TENANT_B, tenantBFlightId);
    }

    @Test
    void getFlight_asOtherTenant_returnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/flights/{id}", tenantBFlightId).with(tenantAViewer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTurnaroundsForFlight_asOtherTenant_returnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/flights/{id}/turnarounds", tenantBFlightId).with(tenantAViewer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRisk_asOtherTenant_returnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/flights/{id}/risk", tenantBFlightId).with(tenantAViewer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignIncident_asOtherTenant_returnsNotFound_andNeverMutatesTheIncident() throws Exception {
        mockMvc.perform(post("/v1/incidents/{id}/assign", tenantBIncidentId)
                        .with(tenantAController())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"someone\"}"))
                .andExpect(status().isNotFound());

        Incident stillOwnedByTenantB = incidentRepository.findByIdAndTenantId(tenantBIncidentId, TENANT_B)
                .orElseThrow();
        assertThat(stillOwnedByTenantB.getStatus()).isEqualTo(Incident.STATUS_OPEN);
        assertThat(stillOwnedByTenantB.getOwner()).isNull();
    }

    @Test
    void resolveIncident_asOtherTenant_returnsNotFound_andNeverMutatesTheIncident() throws Exception {
        mockMvc.perform(post("/v1/incidents/{id}/resolve", tenantBIncidentId).with(tenantAController()))
                .andExpect(status().isNotFound());

        Incident stillOwnedByTenantB = incidentRepository.findByIdAndTenantId(tenantBIncidentId, TENANT_B)
                .orElseThrow();
        assertThat(stillOwnedByTenantB.getStatus()).isEqualTo(Incident.STATUS_OPEN);
    }

    private RequestPostProcessor tenantAViewer() {
        return jwt().jwt(builder -> builder.claim("tenant_id", TENANT_A))
                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }

    private RequestPostProcessor tenantAController() {
        return jwt().jwt(builder -> builder.claim("tenant_id", TENANT_A).claim("preferred_username", "demo.controller"))
                .authorities(new SimpleGrantedAuthority("ROLE_CONTROLLER"));
    }

    private void seedTenant(String tenantId) {
        entityManager.createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, tenantId)
                .executeUpdate();
    }

    private UUID seedFlight(String tenantId, String externalId) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                "INSERT INTO flight_leg (id, tenant_id, external_id, source_system, carrier, flight_number, " +
                        "service_date, origin, destination, lifecycle, version) " +
                        "VALUES (?1, ?2, ?3, 'SIMULATOR', 'SA', 'SA0000', ?4, 'CPT', 'JNB', 'SCHEDULED', 0)")
                .setParameter(1, id)
                .setParameter(2, tenantId)
                .setParameter(3, externalId)
                .setParameter(4, LocalDate.of(2026, 9, 29))
                .executeUpdate();
        return id;
    }

    private UUID seedIncident(String tenantId, UUID flightLegId) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                "INSERT INTO incident (id, tenant_id, flight_leg_id, category, status, description) " +
                        "VALUES (?1, ?2, ?3, 'BAGGAGE_DELAY', 'OPEN', 'seed')")
                .setParameter(1, id)
                .setParameter(2, tenantId)
                .setParameter(3, flightLegId)
                .executeUpdate();
        return id;
    }
}
