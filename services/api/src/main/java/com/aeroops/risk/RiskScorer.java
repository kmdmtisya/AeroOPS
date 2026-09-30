package com.aeroops.risk;

import com.aeroops.flights.FlightLeg;
import com.aeroops.incidents.Incident;
import com.aeroops.stands.StandAssignment;
import com.aeroops.turnarounds.Task;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rule-based, explainable delay-risk scoring (roadmap Phase 5, F08). Deliberately a plain
 * class with no Spring/DB dependency — every input is passed in explicitly and {@code now}
 * is a parameter rather than {@code Instant.now()}, so the whole thing is a pure function
 * and can be unit-tested without a Spring context (a new precedent in this codebase; every
 * prior test is {@code @SpringBootTest}-based).
 *
 * Never produces a recommendation or triggers a side effect — only a level plus the reasons
 * behind it, per the roadmap's "show reasons and contributing fields; no autonomous action."
 */
public class RiskScorer {

    private final RiskThresholdsProperties thresholds;

    public RiskScorer(RiskThresholdsProperties thresholds) {
        this.thresholds = thresholds;
    }

    public RiskAssessment assess(FlightLeg flight, List<Task> tasks, List<Incident> incidents,
                                  StandAssignment standAssignment, Instant now) {
        List<RiskReason> reasons = new ArrayList<>();
        RiskLevel level = RiskLevel.LOW;

        RiskLevel delayLevel = onBlockDelayReason(flight, now).map(reason -> {
            reasons.add(reason);
            return reason.thresholdBreached().equals("critical") ? RiskLevel.HIGH : RiskLevel.MEDIUM;
        }).orElse(RiskLevel.LOW);
        level = highest(level, delayLevel);

        for (Task task : tasks) {
            overdueTaskReason(task, now).ifPresent(reasons::add);
        }
        if (reasons.stream().anyMatch(r -> r.field().equals("task.expectedAt"))) {
            level = highest(level, RiskLevel.MEDIUM);
        }

        if (thresholds.isIncidentSignalEnabled()) {
            for (Incident incident : incidents) {
                openIncidentReason(incident).ifPresent(reasons::add);
            }
            if (reasons.stream().anyMatch(r -> r.field().equals("incident.status"))) {
                level = highest(level, RiskLevel.MEDIUM);
            }
        }

        if (thresholds.isStandOverrideSignalEnabled()) {
            standOverrideReason(standAssignment).ifPresent(reasons::add);
            if (reasons.stream().anyMatch(r -> r.field().equals("standAssignment.override"))) {
                level = highest(level, RiskLevel.MEDIUM);
            }
        }

        boolean lowConfidence = isStale(flight, now);

        return new RiskAssessment(level, reasons, lowConfidence);
    }

    private Optional<RiskReason> onBlockDelayReason(FlightLeg flight, Instant now) {
        Instant scheduled = flight.getScheduledOnBlock();
        if (scheduled == null) {
            return Optional.empty();
        }
        Instant observed = flight.getActualOnBlock() != null ? flight.getActualOnBlock() : flight.getEstimatedOnBlock();
        if (observed == null) {
            return Optional.empty();
        }
        long delayMinutes = Duration.between(scheduled, observed).toMinutes();
        if (delayMinutes >= thresholds.getOnBlockDelayCriticalMinutes()) {
            return Optional.of(new RiskReason(
                    "onBlock", "critical", delayMinutes + " min",
                    "On-block delay of " + delayMinutes + " min meets or exceeds the critical threshold of "
                            + thresholds.getOnBlockDelayCriticalMinutes() + " min"));
        }
        if (delayMinutes >= thresholds.getOnBlockDelayWarningMinutes()) {
            return Optional.of(new RiskReason(
                    "onBlock", "warning", delayMinutes + " min",
                    "On-block delay of " + delayMinutes + " min meets or exceeds the warning threshold of "
                            + thresholds.getOnBlockDelayWarningMinutes() + " min"));
        }
        return Optional.empty();
    }

    private Optional<RiskReason> overdueTaskReason(Task task, Instant now) {
        if (!Task.STATUS_PENDING.equals(task.getStatus()) || task.getExpectedAt() == null) {
            return Optional.empty();
        }
        if (task.getExpectedAt().isAfter(now)) {
            return Optional.empty();
        }
        long overdueMinutes = Duration.between(task.getExpectedAt(), now).toMinutes();
        if (overdueMinutes < thresholds.getTaskOverdueMinutes()) {
            return Optional.empty();
        }
        return Optional.of(new RiskReason(
                "task.expectedAt", thresholds.getTaskOverdueMinutes() + " min", overdueMinutes + " min",
                "Task \"" + task.getName() + "\" is " + overdueMinutes + " min overdue"));
    }

    private Optional<RiskReason> openIncidentReason(Incident incident) {
        if (!Incident.STATUS_OPEN.equals(incident.getStatus()) && !Incident.STATUS_ASSIGNED.equals(incident.getStatus())) {
            return Optional.empty();
        }
        return Optional.of(new RiskReason(
                "incident.status", "OPEN or ASSIGNED", incident.getStatus(),
                "Incident \"" + incident.getCategory() + "\" is " + incident.getStatus()));
    }

    private Optional<RiskReason> standOverrideReason(StandAssignment standAssignment) {
        if (standAssignment == null || !standAssignment.isOverride()) {
            return Optional.empty();
        }
        return Optional.of(new RiskReason(
                "standAssignment.override", "true", "true",
                "Stand assignment was forced despite a conflict with another active assignment"));
    }

    private boolean isStale(FlightLeg flight, Instant now) {
        if (flight.getUpdatedAt() == null) {
            return false;
        }
        return Duration.between(flight.getUpdatedAt(), now).toMillis() > STALENESS_THRESHOLD_MS;
    }

    /** Mirrors apps/web/src/format.ts's STALENESS_THRESHOLD_MS (5 minutes) exactly. */
    private static final long STALENESS_THRESHOLD_MS = 5 * 60 * 1000;

    private static RiskLevel highest(RiskLevel a, RiskLevel b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
