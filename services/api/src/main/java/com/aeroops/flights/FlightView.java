package com.aeroops.flights;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API projection of a FlightLeg. Keeps scheduled/estimated/actual explicitly separate
 * per docs/AEROOPS_SYSTEM_DESIGN.md section 5 — never collapse them into one "time" field.
 */
public record FlightView(
        UUID id,
        String carrier,
        String flightNumber,
        LocalDate serviceDate,
        String origin,
        String destination,
        Instant scheduledOnBlock,
        Instant estimatedOnBlock,
        Instant actualOnBlock,
        Instant scheduledOffBlock,
        Instant estimatedOffBlock,
        Instant actualOffBlock,
        String lifecycle,
        String sourceSystem,
        Instant updatedAt
) {

    public static FlightView from(FlightLeg leg) {
        return new FlightView(
                leg.getId(),
                leg.getCarrier(),
                leg.getFlightNumber(),
                leg.getServiceDate(),
                leg.getOrigin(),
                leg.getDestination(),
                leg.getScheduledOnBlock(),
                leg.getEstimatedOnBlock(),
                leg.getActualOnBlock(),
                leg.getScheduledOffBlock(),
                leg.getEstimatedOffBlock(),
                leg.getActualOffBlock(),
                leg.getLifecycle(),
                leg.getSourceSystem(),
                leg.getUpdatedAt()
        );
    }
}
