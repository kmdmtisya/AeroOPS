package com.aeroops.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Mirrors packages/contracts/event-envelope.schema.json. tenant_id in the JSON body is
 * accepted for shape-completeness only and is never trusted for authorization — the
 * actual tenant scope always comes from {@link com.aeroops.tenancy.TenantContext}
 * (bound from the caller's JWT), per the schema's own description of that field.
 */
public record InboundEventEnvelopeRequest(
        @JsonProperty("event_id") String eventId,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("source") String source,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("received_at") Instant receivedAt,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("payload") JsonNode payload
) {

    /** Returns a rejection reason for a structurally invalid envelope, or null if it's well-formed. */
    public String validate() {
        if (isBlank(eventId)) return "event_id is required";
        if (isBlank(source)) return "source is required";
        if (isBlank(eventType)) return "event_type is required";
        if (isBlank(schemaVersion)) return "schema_version is required";
        if (occurredAt == null) return "occurred_at is required";
        if (receivedAt == null) return "received_at is required";
        if (payload == null || payload.isNull()) return "payload is required";
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
