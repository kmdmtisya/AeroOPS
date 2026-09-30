package com.aeroops.risk;

import com.aeroops.flights.FlightLeg;
import com.aeroops.flights.FlightLegRepository;
import com.aeroops.incidents.Incident;
import com.aeroops.incidents.IncidentRepository;
import com.aeroops.stands.StandAssignment;
import com.aeroops.stands.StandAssignmentRepository;
import com.aeroops.tenancy.TenantContext;
import com.aeroops.turnarounds.Task;
import com.aeroops.turnarounds.TaskRepository;
import com.aeroops.turnarounds.Turnaround;
import com.aeroops.turnarounds.TurnaroundRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only rule-based delay-risk endpoint (roadmap Phase 5, F08). Tenant is always resolved
 * from {@link TenantContext}, never a request parameter or path segment — same rule as
 * {@link com.aeroops.flights.FlightController}. No @PreAuthorize restriction: read-only,
 * same as the other GET endpoints in this codebase.
 */
@RestController
@RequestMapping("/v1/flights")
public class RiskController {

    private final FlightLegRepository flightLegRepository;
    private final TurnaroundRepository turnaroundRepository;
    private final TaskRepository taskRepository;
    private final IncidentRepository incidentRepository;
    private final StandAssignmentRepository standAssignmentRepository;
    private final RiskScorer riskScorer;

    public RiskController(FlightLegRepository flightLegRepository,
                           TurnaroundRepository turnaroundRepository,
                           TaskRepository taskRepository,
                           IncidentRepository incidentRepository,
                           StandAssignmentRepository standAssignmentRepository,
                           RiskThresholdsProperties riskThresholdsProperties) {
        this.flightLegRepository = flightLegRepository;
        this.turnaroundRepository = turnaroundRepository;
        this.taskRepository = taskRepository;
        this.incidentRepository = incidentRepository;
        this.standAssignmentRepository = standAssignmentRepository;
        this.riskScorer = new RiskScorer(riskThresholdsProperties);
    }

    @GetMapping("/{id}/risk")
    public ResponseEntity<RiskView> risk(@PathVariable UUID id) {
        String tenantId = TenantContext.get();
        Optional<FlightLeg> flight = flightLegRepository.findByIdAndTenantId(id, tenantId);
        if (flight.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Task> tasks = turnaroundRepository.findByFlightIdAndTenantId(id, tenantId)
                .map(Turnaround::getId)
                .map(turnaroundId -> taskRepository.findByTurnaroundIdAndTenantIdOrderBySequenceAsc(turnaroundId, tenantId))
                .orElseGet(List::of);

        List<Incident> incidents = incidentRepository.findByFlightLegIdAndTenantId(id, tenantId);

        StandAssignment standAssignment = standAssignmentRepository
                .findByFlightIdAndTenantIdAndStatus(id, tenantId, StandAssignment.STATUS_ACTIVE)
                .orElse(null);

        RiskAssessment assessment = riskScorer.assess(flight.get(), tasks, incidents, standAssignment, Instant.now());
        return ResponseEntity.ok(RiskView.from(assessment));
    }
}
