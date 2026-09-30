package com.aeroops.incidents;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 3 Step 3.5 exit-gate claim from docs/AEROOPS_EXECUTION_PLAN.md:
 * create -> assign -> resolve produces exactly one AuditEvent per transition, each with
 * the correct actor/action, and the incident ends up RESOLVED with resolvedAt set.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class IncidentServiceTest {

    private static final String TENANT = "demo-airport";
    private static final String ACTOR = "demo.controller";

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seedTenantAndBindContext() {
        entityManager.createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, TENANT)
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
    void createAssignResolve_writesThreeAuditEvents_andEndsResolved() {
        Incident created = incidentService.create(
                new CreateIncidentRequest("BAGGAGE_DELAY", "Late bags on SA1234", null, null));
        assertThat(created.getStatus()).isEqualTo(Incident.STATUS_OPEN);

        incidentService.assign(created.getId(), "handler.jane");
        Incident resolved = incidentService.resolve(created.getId());

        assertThat(resolved.getStatus()).isEqualTo(Incident.STATUS_RESOLVED);
        assertThat(resolved.getResolvedAt()).isNotNull();

        List<AuditEvent> events = auditEventRepository.findByTenantIdOrderByOccurredAtAsc(TENANT);
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getAction()).isEqualTo("INCIDENT_CREATED");
        assertThat(events.get(1).getAction()).isEqualTo("INCIDENT_ASSIGNED");
        assertThat(events.get(2).getAction()).isEqualTo("INCIDENT_RESOLVED");
        assertThat(events).allSatisfy(e -> assertThat(e.getActor()).isEqualTo(ACTOR));

        Incident persisted = incidentRepository.findByIdAndTenantId(created.getId(), TENANT).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(Incident.STATUS_RESOLVED);
    }
}
