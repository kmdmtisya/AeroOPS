package com.aeroops.incidents;

import java.time.Instant;
import java.util.UUID;

public record IncidentView(
        UUID id,
        UUID flightLegId,
        UUID standId,
        String category,
        String status,
        String owner,
        String description,
        Instant openedAt,
        Instant resolvedAt
) {

    public static IncidentView from(Incident incident) {
        return new IncidentView(
                incident.getId(),
                incident.getFlightLegId(),
                incident.getStandId(),
                incident.getCategory(),
                incident.getStatus(),
                incident.getOwner(),
                incident.getDescription(),
                incident.getOpenedAt(),
                incident.getResolvedAt()
        );
    }
}
