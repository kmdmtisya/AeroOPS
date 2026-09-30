package com.aeroops.incidents;

import com.aeroops.config.TestJwtDecoderConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the Phase 4 Step 4.2 exit-gate claim: a viewer-role token is authenticated (tenant
 * claim resolves fine) but forbidden from a mutating endpoint, while a controller-role token
 * with the exact same tenant succeeds on the same endpoint. Uses MockMvc + spring-security-test's
 * jwt() request post-processor with explicit .authorities(...) to stand in for a real Keycloak
 * token's realm_access.roles → ROLE_... mapping (RealmRoleConverter's own unit-level behavior is
 * exercised live against real Keycloak tokens, not here — see docs/AEROOPS_EXECUTION_PLAN_PHASE4.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class IncidentAuthorizationTest {

    private static final String TENANT = "demo-airport";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seedTenant() {
        entityManager.createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, TENANT)
                .executeUpdate();
    }

    @Test
    void viewerRole_isForbiddenFromCreatingIncident() throws Exception {
        mockMvc.perform(post("/v1/incidents")
                        .with(jwt()
                                .jwt(builder -> builder.claim("tenant_id", TENANT).claim("preferred_username", "demo.viewer"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateIncidentRequest("BAGGAGE_DELAY", "Late bags", null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_canReadIncidentList() throws Exception {
        mockMvc.perform(get("/v1/incidents")
                        .with(jwt()
                                .jwt(builder -> builder.claim("tenant_id", TENANT).claim("preferred_username", "demo.viewer"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk());
    }

    @Test
    void controllerRole_canCreateIncident() throws Exception {
        mockMvc.perform(post("/v1/incidents")
                        .with(jwt()
                                .jwt(builder -> builder.claim("tenant_id", TENANT).claim("preferred_username", "demo.controller"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CONTROLLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateIncidentRequest("BAGGAGE_DELAY", "Late bags", null, null))))
                .andExpect(status().isCreated());
    }
}
