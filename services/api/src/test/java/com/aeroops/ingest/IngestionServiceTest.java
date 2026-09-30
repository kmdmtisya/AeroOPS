package com.aeroops.ingest;

import com.aeroops.config.TestJwtDecoderConfig;
import com.aeroops.flights.FlightLeg;
import com.aeroops.flights.FlightLegRepository;
import com.aeroops.tenancy.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 3 Step 3.1/3.2 exit-gate claims from docs/AEROOPS_EXECUTION_PLAN.md:
 * an accepted event creates/updates a FlightLeg, a replayed event with the same
 * (tenant, source, event_id) is a no-op DUPLICATE with no second mutation, and a
 * malformed payload is rejected with a stored reason rather than silently dropped.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class IngestionServiceTest {

    private static final String TENANT = "demo-airport";

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private FlightLegRepository flightLegRepository;

    @Autowired
    private SourceEventRepository sourceEventRepository;

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
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void acceptedEvent_createsFlightLeg_andReplayIsIdempotent() throws Exception {
        InboundEventEnvelopeRequest envelope = envelope("evt-0001", "flight.update",
                "2026-09-29T06:45:00Z", flightPayload("SIM-FL-1001", "TURNAROUND"));

        IngestResult first = ingestionService.ingest(envelope);
        assertThat(first.status()).isEqualTo(SourceEvent.STATUS_ACCEPTED);

        Optional<FlightLeg> leg = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATOR", "SIM-FL-1001");
        assertThat(leg).isPresent();
        assertThat(leg.get().getLifecycle()).isEqualTo("TURNAROUND");

        IngestResult replay = ingestionService.ingest(envelope);
        assertThat(replay.status()).isEqualTo(SourceEvent.STATUS_DUPLICATE);

        assertThat(flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATOR", "SIM-FL-1001")).hasValueSatisfying(
                l -> assertThat(l.getId()).isEqualTo(leg.get().getId()));
        assertThat(sourceEventRepository.findAll()).hasSize(1);
    }

    @Test
    void laterEvent_updatesLifecycleAndActualTime_withoutRegressingUntouchedFields() throws Exception {
        ingestionService.ingest(envelope("evt-1001", "flight.update",
                "2026-09-29T06:45:00Z", flightPayload("SIM-FL-2001", "INBOUND")));

        String updatePayload = """
                {"external_id":"SIM-FL-2001","carrier":"SA","flight_number":"SA1234",
                 "service_date":"2026-09-29","origin":"CPT","destination":"JNB",
                 "actual_on_block":"2026-09-29T07:03:00Z","lifecycle":"ON_BLOCK"}
                """;
        ingestionService.ingest(envelope("evt-1002", "flight.update", "2026-09-29T07:03:00Z", updatePayload));

        FlightLeg leg = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATOR", "SIM-FL-2001").orElseThrow();
        assertThat(leg.getLifecycle()).isEqualTo("ON_BLOCK");
        assertThat(leg.getActualOnBlock()).isEqualTo(Instant.parse("2026-09-29T07:03:00Z"));
    }

    @Test
    void outOfOrderEvent_isIgnored_andDoesNotRegressAlreadyAppliedState() throws Exception {
        ingestionService.ingest(envelope("evt-2001", "flight.update",
                "2026-09-29T07:03:00Z", flightPayload("SIM-FL-3001", "ON_BLOCK")));

        String stalePayload = """
                {"external_id":"SIM-FL-3001","carrier":"SA","flight_number":"SA1234",
                 "service_date":"2026-09-29","origin":"CPT","destination":"JNB","lifecycle":"SCHEDULED"}
                """;
        IngestResult stale = ingestionService.ingest(
                envelope("evt-2000", "flight.update", "2026-09-29T06:00:00Z", stalePayload));
        assertThat(stale.status()).isEqualTo(SourceEvent.STATUS_ACCEPTED);

        FlightLeg leg = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATOR", "SIM-FL-3001").orElseThrow();
        assertThat(leg.getLifecycle()).isEqualTo("ON_BLOCK");
    }

    @Test
    void malformedPayload_isRejectedWithReason_andNoFlightLegIsCreated() throws Exception {
        String badPayload = """
                {"external_id":"SIM-FL-9999","carrier":"SA","flight_number":"SA1234",
                 "service_date":"2026-09-29","origin":"CPT","destination":"JNB","lifecycle":"NOT_A_REAL_STATE"}
                """;
        IngestResult result = ingestionService.ingest(
                envelope("evt-9999", "flight.update", "2026-09-29T06:45:00Z", badPayload));

        assertThat(result.status()).isEqualTo(SourceEvent.STATUS_REJECTED);
        assertThat(result.reason()).contains("lifecycle");
        assertThat(flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                TENANT, "SIMULATOR", "SIM-FL-9999")).isEmpty();

        SourceEvent stored = sourceEventRepository.findById(result.sourceEventId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(SourceEvent.STATUS_REJECTED);
        assertThat(stored.getRejectReason()).contains("lifecycle");
    }

    private InboundEventEnvelopeRequest envelope(String eventId, String eventType, String occurredAt,
                                                  String payloadJson) throws Exception {
        JsonNode payload = objectMapper.readTree(payloadJson);
        return new InboundEventEnvelopeRequest(
                eventId, TENANT, "SIMULATOR", eventType, "1.0",
                Instant.parse(occurredAt), Instant.now(), "corr-" + eventId, payload);
    }

    private String flightPayload(String externalId, String lifecycle) {
        return """
                {"external_id":"%s","carrier":"SA","flight_number":"SA1234",
                 "service_date":"2026-09-29","origin":"CPT","destination":"JNB","lifecycle":"%s"}
                """.formatted(externalId, lifecycle);
    }
}
