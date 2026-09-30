package com.aeroops.ingest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable record of every ingestion attempt, accepted or not — this is both the
 * idempotency ledger (uq_source_event_dedupe on tenant_id/source/external_event_id,
 * see V2__stub_future_modules.sql) and the "review queue" the roadmap calls for:
 * a rejected event is queryable here with its reason, rather than silently dropped.
 */
@Entity
@Table(name = "source_event")
public class SourceEvent {

    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DUPLICATE = "DUPLICATE";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String source;

    @Column(name = "external_event_id", nullable = false)
    private String externalEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private String schemaVersion;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "payload_checksum", nullable = false)
    private String payloadChecksum;

    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private String status;

    @Column(name = "reject_reason")
    private String rejectReason;

    protected SourceEvent() {
        // JPA
    }

    private SourceEvent(UUID id, String tenantId, String source, String externalEventId, String eventType,
                         String schemaVersion, Instant receivedAt, Instant occurredAt, String correlationId,
                         String payloadChecksum, String payload, String status, String rejectReason) {
        this.id = id;
        this.tenantId = tenantId;
        this.source = source;
        this.externalEventId = externalEventId;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.receivedAt = receivedAt;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.payloadChecksum = payloadChecksum;
        this.payload = payload;
        this.status = status;
        this.rejectReason = rejectReason;
    }

    public static SourceEvent accepted(String tenantId, InboundEventEnvelopeRequest envelope,
                                        String payloadChecksum, String rawPayload) {
        return new SourceEvent(UUID.randomUUID(), tenantId, envelope.source(), envelope.eventId(),
                envelope.eventType(), envelope.schemaVersion(), envelope.receivedAt(), envelope.occurredAt(),
                envelope.correlationId(), payloadChecksum, rawPayload, STATUS_ACCEPTED, null);
    }

    public static SourceEvent rejected(String tenantId, InboundEventEnvelopeRequest envelope,
                                        String payloadChecksum, String rawPayload, String reason) {
        return new SourceEvent(UUID.randomUUID(), tenantId, envelope.source(), envelope.eventId(),
                envelope.eventType(), envelope.schemaVersion(), envelope.receivedAt(), envelope.occurredAt(),
                envelope.correlationId(), payloadChecksum, rawPayload, STATUS_REJECTED, reason);
    }

    public UUID getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectReason() {
        return rejectReason;
    }
}
