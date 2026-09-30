package com.aeroops.flights;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the exit-gate claim from docs/AEROOPS_DEVELOPMENT_ROADMAP.md Phase 2:
 * a query scoped to one tenant never returns another tenant's row, even when both
 * rows exist in the same table and the caller supplies the same primary key.
 */
@DataJpaTest
// @DataJpaTest auto-replaces the datasource with its own embedded H2, bypassing the
// custom JDBC URL (and its TIMESTAMPTZ-domain fix) in application-test.yml. Keep the
// configured datasource instead so Flyway's V1/V2/V3 migrations actually run here too.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FlightLegRepositoryTenantIsolationTest {

    @Autowired
    private FlightLegRepository flightLegRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void listByTenant_neverReturnsAnotherTenantsFlight() {
        seedTenant("tenant-a");
        seedTenant("tenant-b");

        UUID flightIdA = seedFlight("tenant-a", "AA-EXT-1");
        seedFlight("tenant-b", "BB-EXT-1");
        entityManager.flush();

        List<FlightLeg> tenantAFlights = flightLegRepository.findByTenantIdOrderByScheduledOnBlockAsc("tenant-a");

        assertThat(tenantAFlights).hasSize(1);
        assertThat(tenantAFlights.get(0).getId()).isEqualTo(flightIdA);
        assertThat(tenantAFlights.get(0).getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void getById_isScopedToTenant_evenForAnExistingId() {
        seedTenant("tenant-a");
        seedTenant("tenant-b");

        UUID flightIdA = seedFlight("tenant-a", "AA-EXT-2");
        entityManager.flush();

        Optional<FlightLeg> asOwningTenant = flightLegRepository.findByIdAndTenantId(flightIdA, "tenant-a");
        Optional<FlightLeg> asOtherTenant = flightLegRepository.findByIdAndTenantId(flightIdA, "tenant-b");

        assertThat(asOwningTenant).isPresent();
        assertThat(asOtherTenant).isEmpty();
    }

    private void seedTenant(String tenantId) {
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                .setParameter(1, tenantId)
                .executeUpdate();
    }

    private UUID seedFlight(String tenantId, String externalId) {
        UUID id = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
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
}
