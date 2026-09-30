package com.aeroops.ingest;

import com.aeroops.flights.FlightEventPayload;
import com.aeroops.flights.FlightLeg;
import com.aeroops.flights.FlightLegRepository;
import com.aeroops.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates, deduplicates and applies inbound events. Dedup key is (tenant_id, source,
 * event_id) — see uq_source_event_dedupe. Every attempt is recorded in source_event
 * regardless of outcome, so a rejected event is auditable rather than silently dropped
 * (this is the roadmap's "review queue" for this phase).
 */
@Service
public class IngestionService {

    private static final String EVENT_TYPE_FLIGHT_UPDATE = "flight.update";

    private final SourceEventRepository sourceEventRepository;
    private final FlightLegRepository flightLegRepository;
    private final ObjectMapper objectMapper;

    public IngestionService(SourceEventRepository sourceEventRepository,
                             FlightLegRepository flightLegRepository,
                             ObjectMapper objectMapper) {
        this.sourceEventRepository = sourceEventRepository;
        this.flightLegRepository = flightLegRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestResult ingest(InboundEventEnvelopeRequest envelope) {
        String tenantId = TenantContext.get();
        String rawPayload = envelope.payload().toString();
        String checksum = sha256(rawPayload);

        String envelopeError = envelope.validate();
        if (envelopeError != null) {
            SourceEvent rejected = sourceEventRepository.save(
                    SourceEvent.rejected(tenantId, envelope, checksum, rawPayload, envelopeError));
            return IngestResult.rejected(envelopeError, rejected.getId());
        }

        Optional<SourceEvent> existing = sourceEventRepository
                .findByTenantIdAndSourceAndExternalEventId(tenantId, envelope.source(), envelope.eventId());
        if (existing.isPresent()) {
            return IngestResult.duplicate(existing.get().getId());
        }

        if (!EVENT_TYPE_FLIGHT_UPDATE.equals(envelope.eventType())) {
            String reason = "unsupported event_type: " + envelope.eventType();
            SourceEvent rejected = sourceEventRepository.save(
                    SourceEvent.rejected(tenantId, envelope, checksum, rawPayload, reason));
            return IngestResult.rejected(reason, rejected.getId());
        }

        FlightEventPayload payload;
        try {
            payload = objectMapper.treeToValue(envelope.payload(), FlightEventPayload.class);
        } catch (Exception e) {
            String reason = "payload does not match flight-event schema: " + e.getMessage();
            SourceEvent rejected = sourceEventRepository.save(
                    SourceEvent.rejected(tenantId, envelope, checksum, rawPayload, reason));
            return IngestResult.rejected(reason, rejected.getId());
        }

        String payloadError = payload.validate();
        if (payloadError != null) {
            SourceEvent rejected = sourceEventRepository.save(
                    SourceEvent.rejected(tenantId, envelope, checksum, rawPayload, payloadError));
            return IngestResult.rejected(payloadError, rejected.getId());
        }

        applyFlightUpdate(tenantId, envelope, payload);

        SourceEvent accepted = sourceEventRepository.save(
                SourceEvent.accepted(tenantId, envelope, checksum, rawPayload));
        return IngestResult.accepted(accepted.getId());
    }

    private void applyFlightUpdate(String tenantId, InboundEventEnvelopeRequest envelope, FlightEventPayload payload) {
        Optional<FlightLeg> existingLeg = flightLegRepository.findByTenantIdAndSourceSystemAndExternalId(
                tenantId, envelope.source(), payload.externalId());

        if (existingLeg.isPresent()) {
            existingLeg.get().applyUpdate(payload, envelope.occurredAt());
        } else {
            FlightLeg leg = FlightLeg.createFromEvent(
                    UUID.randomUUID(), tenantId, envelope.source(), payload, envelope.occurredAt());
            flightLegRepository.save(leg);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
