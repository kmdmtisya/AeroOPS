package com.aeroops.incidents;

import java.util.UUID;

public record CreateIncidentRequest(String category, String description, UUID flightLegId, UUID standId) {
}
