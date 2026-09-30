package com.aeroops.feed;

import com.aeroops.flights.FlightEventPayload;
import com.aeroops.ingest.IngestResult;
import com.aeroops.ingest.IngestionService;
import com.aeroops.ingest.InboundEventEnvelopeRequest;
import com.aeroops.tenancy.ActorContext;
import com.aeroops.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for "connect one real feed" (roadmap Phase 4) with a synthetic one: on each tick,
 * advances one flight in a small rotating pool one step through the lifecycle and pushes it
 * through {@link IngestionService#ingest} exactly like a real adapter would, so the dashboard
 * has live-changing data without a human posting events by hand. Dev-profile only, gated by
 * {@link SimulatedFeedProperties#isEnabled()}.
 */
@Component
public class SimulatedFeedScheduler {

    private static final Logger log = LoggerFactory.getLogger(SimulatedFeedScheduler.class);

    private static final List<String> LIFECYCLE_ORDER = List.of(
            "SCHEDULED", "INBOUND", "ON_BLOCK", "TURNAROUND", "READY", "OFF_BLOCK");

    private static final List<SyntheticFlight> POOL = List.of(
            new SyntheticFlight("SIM-FEED-2001", "SA", "SA2001", "CPT", "JNB"),
            new SyntheticFlight("SIM-FEED-2002", "SA", "SA2002", "DUR", "JNB"),
            new SyntheticFlight("SIM-FEED-2003", "SA", "SA2003", "JNB", "CPT"));

    private final IngestionService ingestionService;
    private final SimulatedFeedProperties properties;
    private final ObjectMapper objectMapper;

    private final AtomicInteger nextFlightIndex = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Integer> lifecycleStageByExternalId = new ConcurrentHashMap<>();

    public SimulatedFeedScheduler(IngestionService ingestionService,
                                   SimulatedFeedProperties properties,
                                   ObjectMapper objectMapper) {
        this.ingestionService = ingestionService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${aeroops.simulated-feed.poll-interval-ms:15000}")
    public void tick() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            TenantContext.set(properties.getTenantId());
            ActorContext.set("simulated-feed");
            IngestResult result = ingestNextStep();
            log.info("Simulated feed tick: status={} sourceEventId={}", result.status(), result.sourceEventId());
        } finally {
            TenantContext.clear();
            ActorContext.clear();
        }
    }

    /**
     * Advances one flight from the pool one lifecycle step and ingests it. Package-visible so
     * tests can invoke a single tick deterministically instead of waiting on the real schedule.
     */
    IngestResult ingestNextStep() {
        SyntheticFlight flight = POOL.get(Math.floorMod(nextFlightIndex.getAndIncrement(), POOL.size()));
        int stage = lifecycleStageByExternalId.merge(flight.externalId(), 0, (oldStage, one) ->
                (oldStage + 1) % LIFECYCLE_ORDER.size());
        String lifecycle = LIFECYCLE_ORDER.get(stage);

        Instant now = Instant.now();
        FlightEventPayload payload = new FlightEventPayload(
                flight.externalId(), flight.carrier(), flight.flightNumber(), LocalDate.now(),
                flight.origin(), flight.destination(),
                now, now, "ON_BLOCK".equals(lifecycle) ? now : null,
                now, now, "OFF_BLOCK".equals(lifecycle) ? now : null,
                lifecycle);

        InboundEventEnvelopeRequest envelope = new InboundEventEnvelopeRequest(
                UUID.randomUUID().toString(), properties.getTenantId(), properties.getSource(),
                "flight.update", "1.0", now, now, "simulated-feed-" + UUID.randomUUID(),
                objectMapper.valueToTree(payload));

        return ingestionService.ingest(envelope);
    }

    private record SyntheticFlight(String externalId, String carrier, String flightNumber,
                                    String origin, String destination) {
    }
}
