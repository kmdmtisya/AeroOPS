package com.aeroops.turnarounds;

import java.util.List;
import java.util.UUID;

public record TurnaroundView(UUID id, UUID flightLegId, String templateName, List<TaskView> tasks) {

    public static TurnaroundView from(Turnaround turnaround, List<TaskView> tasks) {
        return new TurnaroundView(turnaround.getId(), turnaround.getFlightId(), turnaround.getTemplateName(), tasks);
    }
}
