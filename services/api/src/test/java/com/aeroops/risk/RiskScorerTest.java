package com.aeroops.risk;

import com.aeroops.flights.FlightEventPayload;
import com.aeroops.flights.FlightLeg;
import com.aeroops.incidents.Incident;
import com.aeroops.stands.StandAssignment;
import com.aeroops.turnarounds.Task;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain JUnit test — no Spring context. RiskScorer is deliberately a pure class (no DB, no
 * Instant.now() inside it), so every rule from docs/AEROOPS_EXECUTION_PLAN_PHASE5.md Step 5.1
 * can be exercised directly. This is a new precedent for this codebase: every other test file
 * is @SpringBootTest-based.
 */
class RiskScorerTest {

    private static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");

    private final RiskThresholdsProperties thresholds = new RiskThresholdsProperties();
    private final RiskScorer scorer = new RiskScorer(thresholds);

    @Test
    void noSignals_isLowRiskWithNoReasons() {
        FlightLeg flight = flightOnSchedule();
        setUpdatedAt(flight, NOW);

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.LOW);
        assertThat(assessment.reasons()).isEmpty();
        assertThat(assessment.lowConfidence()).isFalse();
    }

    @Test
    void onBlockDelay_belowWarningThreshold_isNotFlagged() {
        FlightLeg flight = flightWithOnBlockDelay(minutes(10));
        setUpdatedAt(flight, NOW);

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.LOW);
        assertThat(assessment.reasons()).isEmpty();
    }

    @Test
    void onBlockDelay_atWarningThreshold_isMediumWithReason() {
        FlightLeg flight = flightWithOnBlockDelay(minutes(15));
        setUpdatedAt(flight, NOW);

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(assessment.reasons()).hasSize(1);
        RiskReason reason = assessment.reasons().get(0);
        assertThat(reason.field()).isEqualTo("onBlock");
        assertThat(reason.thresholdBreached()).isEqualTo("warning");
        assertThat(reason.actualValue()).isEqualTo("15 min");
    }

    @Test
    void onBlockDelay_atCriticalThreshold_isHigh() {
        FlightLeg flight = flightWithOnBlockDelay(minutes(45));
        setUpdatedAt(flight, NOW);

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(assessment.reasons().get(0).thresholdBreached()).isEqualTo("critical");
    }

    @Test
    void overduePendingTask_isMediumWithReason() {
        FlightLeg flight = flightOnSchedule();
        setUpdatedAt(flight, NOW);
        Task overdueTask = Task.create("demo-airport", UUID.randomUUID(), "Baggage unload", 1);
        setExpectedAt(overdueTask, NOW.minus(30, ChronoUnit.MINUTES));

        RiskAssessment assessment = scorer.assess(flight, List.of(overdueTask), List.of(), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(assessment.reasons()).anySatisfy(r -> assertThat(r.field()).isEqualTo("task.expectedAt"));
    }

    @Test
    void completedTask_pastExpectedAt_isNotFlagged() {
        FlightLeg flight = flightOnSchedule();
        setUpdatedAt(flight, NOW);
        Task completedTask = Task.create("demo-airport", UUID.randomUUID(), "Baggage unload", 1);
        setExpectedAt(completedTask, NOW.minus(30, ChronoUnit.MINUTES));
        completedTask.markComplete();

        RiskAssessment assessment = scorer.assess(flight, List.of(completedTask), List.of(), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.LOW);
        assertThat(assessment.reasons()).isEmpty();
    }

    @Test
    void openIncident_isMediumWithReason() {
        FlightLeg flight = flightOnSchedule();
        setUpdatedAt(flight, NOW);
        Incident incident = Incident.open("demo-airport", flight.getId(), null, "BAGGAGE_DELAY", "Late bags");

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(incident), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(assessment.reasons()).anySatisfy(r -> assertThat(r.field()).isEqualTo("incident.status"));
    }

    @Test
    void resolvedIncident_isNotFlagged() {
        FlightLeg flight = flightOnSchedule();
        setUpdatedAt(flight, NOW);
        Incident incident = Incident.open("demo-airport", flight.getId(), null, "BAGGAGE_DELAY", "Late bags");
        incident.resolve();

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(incident), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.LOW);
        assertThat(assessment.reasons()).isEmpty();
    }

    @Test
    void standAssignmentOverride_isMediumWithReason() {
        FlightLeg flight = flightOnSchedule();
        setUpdatedAt(flight, NOW);
        StandAssignment assignment = StandAssignment.create(
                "demo-airport", flight.getId(), UUID.randomUUID(), NOW, NOW.plusSeconds(3600), true);

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(), assignment, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(assessment.reasons()).anySatisfy(r -> assertThat(r.field()).isEqualTo("standAssignment.override"));
    }

    @Test
    void nonOverrideStandAssignment_isNotFlagged() {
        FlightLeg flight = flightOnSchedule();
        setUpdatedAt(flight, NOW);
        StandAssignment assignment = StandAssignment.create(
                "demo-airport", flight.getId(), UUID.randomUUID(), NOW, NOW.plusSeconds(3600), false);

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(), assignment, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.LOW);
        assertThat(assessment.reasons()).isEmpty();
    }

    @Test
    void staleFlightData_isMarkedLowConfidence_regardlessOfLevel() {
        FlightLeg flight = flightOnSchedule();
        setUpdatedAt(flight, NOW.minus(10, ChronoUnit.MINUTES));

        RiskAssessment assessment = scorer.assess(flight, List.of(), List.of(), null, NOW);

        assertThat(assessment.lowConfidence()).isTrue();
        assertThat(assessment.level()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void multipleSignals_combineToTheHighestLevel_withAllReasonsPresent() {
        FlightLeg flight = flightWithOnBlockDelay(minutes(45));
        setUpdatedAt(flight, NOW);
        Task overdueTask = Task.create("demo-airport", UUID.randomUUID(), "Baggage unload", 1);
        setExpectedAt(overdueTask, NOW.minus(30, ChronoUnit.MINUTES));
        Incident incident = Incident.open("demo-airport", flight.getId(), null, "BAGGAGE_DELAY", "Late bags");

        RiskAssessment assessment = scorer.assess(flight, List.of(overdueTask), List.of(incident), null, NOW);

        assertThat(assessment.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(assessment.reasons()).hasSize(3);
    }

    private long minutes(long minutes) {
        return minutes;
    }

    private FlightLeg flightOnSchedule() {
        FlightEventPayload payload = new FlightEventPayload(
                "SA-EXT-1", "SA", "SA100", LocalDate.of(2026, 9, 30), "CPT", "JNB",
                NOW, NOW, NOW, NOW.plusSeconds(3600), NOW.plusSeconds(3600), null,
                "ON_BLOCK");
        return FlightLeg.createFromEvent(UUID.randomUUID(), "demo-airport", "SIMULATED_AOCC", payload, NOW);
    }

    private FlightLeg flightWithOnBlockDelay(long delayMinutes) {
        Instant scheduled = NOW.minus(delayMinutes, ChronoUnit.MINUTES);
        FlightEventPayload payload = new FlightEventPayload(
                "SA-EXT-1", "SA", "SA100", LocalDate.of(2026, 9, 30), "CPT", "JNB",
                scheduled, null, NOW, scheduled.plusSeconds(3600), null, null,
                "ON_BLOCK");
        return FlightLeg.createFromEvent(UUID.randomUUID(), "demo-airport", "SIMULATED_AOCC", payload, NOW);
    }

    private static void setUpdatedAt(FlightLeg flight, Instant value) {
        setField(flight, "updatedAt", value);
    }

    private static void setExpectedAt(Task task, Instant value) {
        setField(task, "expectedAt", value);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
