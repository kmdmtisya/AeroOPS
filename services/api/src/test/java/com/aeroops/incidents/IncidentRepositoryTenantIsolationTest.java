package com.aeroops.incidents;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same proof as FlightLegRepositoryTenantIsolationTest, applied to incidents: a query
 * scoped to one tenant never returns another tenant's row.
 */
@DataJpaTest
// @DataJpaTest auto-replaces the datasource with its own embedded H2, bypassing the
// custom JDBC URL (and its TIMESTAMPTZ-domain fix) in application-test.yml. Keep the
// configured datasource instead so Flyway's migrations actually run here too.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class IncidentRepositoryTenantIsolationTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void listByTenant_neverReturnsAnotherTenantsIncident() {
        seedTenant("tenant-a");
        seedTenant("tenant-b");

        UUID incidentIdA = seedIncident("tenant-a", "BAGGAGE_DELAY");
        seedIncident("tenant-b", "STAND_CONFLICT");
        entityManager.flush();

        List<Incident> tenantAIncidents = incidentRepository.findByTenantIdOrderByOpenedAtDesc("tenant-a");

        assertThat(tenantAIncidents).hasSize(1);
        assertThat(tenantAIncidents.get(0).getId()).isEqualTo(incidentIdA);
        assertThat(tenantAIncidents.get(0).getTenantId()).isEqualTo("tenant-a");
    }

    private void seedTenant(String tenantId) {
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, tenantId)
                .executeUpdate();
    }

    private UUID seedIncident(String tenantId, String category) {
        UUID id = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO incident (id, tenant_id, category, status, description) " +
                        "VALUES (?1, ?2, ?3, 'OPEN', 'seed')")
                .setParameter(1, id)
                .setParameter(2, tenantId)
                .setParameter(3, category)
                .executeUpdate();
        return id;
    }
}
