package com.aeroops.flights;

import com.aeroops.tenancy.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant is always resolved from {@link TenantContext} (bound from the caller's JWT),
 * never from a request parameter or path segment — see docs/AEROOPS_SYSTEM_DESIGN.md section 5.
 */
@RestController
@RequestMapping("/v1/flights")
public class FlightController {

    private final FlightLegRepository flightLegRepository;

    public FlightController(FlightLegRepository flightLegRepository) {
        this.flightLegRepository = flightLegRepository;
    }

    @GetMapping
    public List<FlightView> list() {
        String tenantId = TenantContext.get();
        return flightLegRepository.findByTenantIdOrderByScheduledOnBlockAsc(tenantId).stream()
                .map(FlightView::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightView> get(@PathVariable UUID id) {
        String tenantId = TenantContext.get();
        return flightLegRepository.findByIdAndTenantId(id, tenantId)
                .map(FlightView::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
