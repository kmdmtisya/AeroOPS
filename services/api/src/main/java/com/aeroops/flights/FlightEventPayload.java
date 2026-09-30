package com.aeroops.flights;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * Canonical flight payload carried inside an InboundEventEnvelope with event_type=flight.update.
 * Mirrors packages/contracts/flight-event.schema.json exactly — keep both in sync by hand,
 * since this module has no schema-validation-from-JSON-Schema wiring yet.
 */
public record FlightEventPayload(
        @JsonProperty("external_id") String externalId,
        @JsonProperty("carrier") String carrier,
        @JsonProperty("flight_number") String flightNumber,
        @JsonProperty("service_date") LocalDate serviceDate,
        @JsonProperty("origin") String origin,
        @JsonProperty("destination") String destination,
        @JsonProperty("scheduled_on_block") Instant scheduledOnBlock,
        @JsonProperty("estimated_on_block") Instant estimatedOnBlock,
        @JsonProperty("actual_on_block") Instant actualOnBlock,
        @JsonProperty("scheduled_off_block") Instant scheduledOffBlock,
        @JsonProperty("estimated_off_block") Instant estimatedOffBlock,
        @JsonProperty("actual_off_block") Instant actualOffBlock,
        @JsonProperty("lifecycle") String lifecycle
) {
    public static final Set<String> VALID_LIFECYCLES = Set.of(
            "SCHEDULED", "INBOUND", "ON_BLOCK", "TURNAROUND", "READY", "OFF_BLOCK", "CANCELLED");

    /**
     * Returns a human-readable reason this payload is invalid, or null if it's acceptable.
     * Manual validation (rather than bean-validation cascading through the envelope's raw
     * JsonNode payload) keeps the rejection reason readable for the source_event audit trail.
     */
    public String validate() {
        if (isBlank(externalId)) return "external_id is required";
        if (isBlank(carrier)) return "carrier is required";
        if (isBlank(flightNumber)) return "flight_number is required";
        if (serviceDate == null) return "service_date is required";
        if (isBlank(origin)) return "origin is required";
        if (isBlank(destination)) return "destination is required";
        if (isBlank(lifecycle)) return "lifecycle is required";
        if (!VALID_LIFECYCLES.contains(lifecycle)) return "lifecycle must be one of " + VALID_LIFECYCLES;
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
