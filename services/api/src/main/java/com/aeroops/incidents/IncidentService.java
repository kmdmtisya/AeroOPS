package com.aeroops.incidents;

import com.aeroops.audit.AuditEvent;
import com.aeroops.audit.AuditEventRepository;
import com.aeroops.tenancy.ActorContext;
import com.aeroops.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Every mutation here writes an AuditEvent in the same transaction — this is the first
 * real writer of the audit_event table (unused since Phase 2, see AuditEvent's own comment).
 */
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AuditEventRepository auditEventRepository;

    public IncidentService(IncidentRepository incidentRepository, AuditEventRepository auditEventRepository) {
        this.incidentRepository = incidentRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public Incident create(CreateIncidentRequest request) {
        String tenantId = TenantContext.get();
        Incident incident = Incident.open(
                tenantId, request.flightLegId(), request.standId(), request.category(), request.description());
        incidentRepository.save(incident);
        audit(tenantId, "INCIDENT_CREATED", incident.getId(), null, incident.getStatus());
        return incident;
    }

    @Transactional
    public Incident assign(UUID id, String owner) {
        String tenantId = TenantContext.get();
        Incident incident = getOwned(id, tenantId);
        String before = incident.getStatus();
        incident.assign(owner);
        audit(tenantId, "INCIDENT_ASSIGNED", incident.getId(), before, incident.getStatus());
        return incident;
    }

    @Transactional
    public Incident resolve(UUID id) {
        String tenantId = TenantContext.get();
        Incident incident = getOwned(id, tenantId);
        String before = incident.getStatus();
        incident.resolve();
        audit(tenantId, "INCIDENT_RESOLVED", incident.getId(), before, incident.getStatus());
        return incident;
    }

    private Incident getOwned(UUID id, String tenantId) {
        return incidentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));
    }

    private void audit(String tenantId, String action, UUID incidentId, String beforeRef, String afterRef) {
        auditEventRepository.save(new AuditEvent(
                UUID.randomUUID(), tenantId, ActorContext.get(), action, "incident:" + incidentId,
                beforeRef, afterRef, null, Instant.now()));
    }
}
