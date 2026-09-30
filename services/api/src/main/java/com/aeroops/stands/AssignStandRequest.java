package com.aeroops.stands;

import java.time.Instant;
import java.util.UUID;

public record AssignStandRequest(UUID flightLegId, UUID standId, Instant windowStart, Instant windowEnd, boolean override) {
}
