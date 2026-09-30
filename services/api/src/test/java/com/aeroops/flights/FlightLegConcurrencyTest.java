package com.aeroops.flights;

import com.aeroops.config.TestJwtDecoderConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 5 Step 5.2 exit-gate claim: FlightLeg's existing @Version column
 * (see FlightLeg.java) actually protects against lost updates when two callers load
 * and save the same row concurrently. Not a bugfix — a proof that already-present
 * optimistic locking works, using two real transactions (TransactionTemplate with
 * its own PlatformTransactionManager) rather than the class-level @Transactional
 * rollback-per-test convention used elsewhere, since that convention would force
 * both "concurrent" writers onto the same connection/transaction and defeat the race.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class FlightLegConcurrencyTest {

    private static final String TENANT = "concurrency-test-tenant";

    @Autowired
    private FlightLegRepository flightLegRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID flightId;

    @BeforeEach
    void seedTenantAndFlight() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        flightId = tx.execute(status -> {
            UUID id = UUID.randomUUID();
            entityManager.createNativeQuery(
                    "INSERT INTO airport_tenant (id, name, timezone, config_version) VALUES (?1, ?1, 'UTC', 1)")
                    .setParameter(1, TENANT)
                    .executeUpdate();
            entityManager.createNativeQuery(
                    "INSERT INTO flight_leg (id, tenant_id, external_id, source_system, carrier, flight_number, " +
                            "service_date, origin, destination, lifecycle, version) " +
                            "VALUES (?1, ?2, 'CONC-EXT-1', 'SIMULATOR', 'SA', 'SA0000', ?3, 'CPT', 'JNB', 'SCHEDULED', 0)")
                    .setParameter(1, id)
                    .setParameter(2, TENANT)
                    .setParameter(3, LocalDate.of(2026, 9, 29))
                    .executeUpdate();
            return id;
        });
    }

    @AfterEach
    void cleanUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery("DELETE FROM flight_leg WHERE tenant_id = ?1")
                    .setParameter(1, TENANT)
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM airport_tenant WHERE id = ?1")
                    .setParameter(1, TENANT)
                    .executeUpdate();
        });
    }

    @Test
    void concurrentUpdates_toTheSameFlightLeg_oneSucceedsAndOneThrowsOptimisticLockException() throws Exception {
        CountDownLatch bothLoaded = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<Optional<Exception>> racerA = racer(bothLoaded, "INBOUND");
            Callable<Optional<Exception>> racerB = racer(bothLoaded, "ON_BLOCK");

            Future<Optional<Exception>> futureA = executor.submit(racerA);
            Future<Optional<Exception>> futureB = executor.submit(racerB);

            Optional<Exception> resultA = futureA.get(10, TimeUnit.SECONDS);
            Optional<Exception> resultB = futureB.get(10, TimeUnit.SECONDS);

            List<Optional<Exception>> results = List.of(resultA, resultB);
            long failureCount = results.stream().filter(Optional::isPresent).count();
            long successCount = results.stream().filter(Optional::isEmpty).count();

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(1);

            Exception failure = results.stream().flatMap(Optional::stream).findFirst().orElseThrow();
            assertThat(isOrWrapsOptimisticLockFailure(failure)).isTrue();

            FlightLeg finalState = flightLegRepository.findById(flightId).orElseThrow();
            assertThat(finalState.getVersion()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Optional<Exception>> racer(CountDownLatch bothLoaded, String lifecycle) {
        return () -> {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            try {
                tx.executeWithoutResult(status -> {
                    FlightLeg leg = flightLegRepository.findById(flightId).orElseThrow();
                    bothLoaded.countDown();
                    try {
                        bothLoaded.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    FlightEventPayload payload = new FlightEventPayload(
                            null, null, null, null, null, null, null, null, null, null, null, null, lifecycle);
                    leg.applyUpdate(payload, Instant.now());
                });
                return Optional.empty();
            } catch (Exception e) {
                return Optional.of(e);
            }
        };
    }

    private static boolean isOrWrapsOptimisticLockFailure(Throwable t) {
        while (t != null) {
            if (t instanceof ObjectOptimisticLockingFailureException) return true;
            t = t.getCause();
        }
        return false;
    }
}
