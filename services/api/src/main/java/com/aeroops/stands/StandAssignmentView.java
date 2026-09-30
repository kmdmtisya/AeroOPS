package com.aeroops.stands;

import java.time.Instant;
import java.util.UUID;

public record StandAssignmentView(UUID id, UUID flightLegId, UUID standId, Instant windowStart,
                                    Instant windowEnd, String status, boolean override) {

    public static StandAssignmentView from(StandAssignment assignment) {
        return new StandAssignmentView(
                assignment.getId(), assignment.getFlightId(), assignment.getStandId(),
                assignment.getWindowStart(), assignment.getWindowEnd(),
                assignment.getStatus(), assignment.isOverride());
    }
}
