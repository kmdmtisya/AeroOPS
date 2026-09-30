package com.aeroops.incidents;

import com.aeroops.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.UUID;

/**
 * Tenant is always resolved from {@link TenantContext}, never from a request parameter
 * or path segment — same rule as {@link com.aeroops.flights.FlightController}.
 */
@RestController
@RequestMapping("/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentRepository incidentRepository;

    public IncidentController(IncidentService incidentService, IncidentRepository incidentRepository) {
        this.incidentService = incidentService;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public List<IncidentView> list() {
        String tenantId = TenantContext.get();
        return incidentRepository.findByTenantIdOrderByOpenedAtDesc(tenantId).stream()
                .map(IncidentView::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CONTROLLER', 'PLANNER', 'TENANT_ADMIN')")
    public ResponseEntity<IncidentView> create(@RequestBody CreateIncidentRequest request) {
        Incident incident = incidentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentView.from(incident));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('CONTROLLER', 'PLANNER', 'TENANT_ADMIN')")
    public IncidentView assign(@PathVariable UUID id, @RequestBody AssignIncidentRequest request) {
        return IncidentView.from(incidentService.assign(id, request.owner()));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('CONTROLLER', 'PLANNER', 'TENANT_ADMIN')")
    public IncidentView resolve(@PathVariable UUID id) {
        return IncidentView.from(incidentService.resolve(id));
    }
}
