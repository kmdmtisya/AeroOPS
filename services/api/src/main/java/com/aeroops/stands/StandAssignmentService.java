package com.aeroops.stands;

import com.aeroops.audit.AuditEvent;
import com.aeroops.audit.AuditEventRepository;
import com.aeroops.tenancy.ActorContext;
import com.aeroops.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class StandAssignmentService {

    private final StandAssignmentRepository standAssignmentRepository;
    private final AuditEventRepository auditEventRepository;

    public StandAssignmentService(StandAssignmentRepository standAssignmentRepository,
                                   AuditEventRepository auditEventRepository) {
        this.standAssignmentRepository = standAssignmentRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public StandAssignmentResult assign(AssignStandRequest request) {
        String tenantId = TenantContext.get();
        List<StandAssignment> overlapping = standAssignmentRepository.findOverlapping(
                tenantId, request.standId(), request.windowStart(), request.windowEnd());

        if (!overlapping.isEmpty() && !request.override()) {
            return StandAssignmentResult.conflict(overlapping);
        }

        StandAssignment assignment = StandAssignment.create(
                tenantId, request.flightLegId(), request.standId(),
                request.windowStart(), request.windowEnd(), request.override());
        standAssignmentRepository.save(assignment);

        String afterRef = assignment.getStatus() + (assignment.isOverride() ? ":OVERRIDE" : "");
        audit(tenantId, "STAND_ASSIGNED", assignment.getId(), null, afterRef);

        return StandAssignmentResult.created(assignment);
    }

    public List<StandAssignment> board(LocalDate serviceDate) {
        String tenantId = TenantContext.get();
        return standAssignmentRepository.findByTenantIdAndServiceDate(tenantId, serviceDate);
    }

    private void audit(String tenantId, String action, UUID assignmentId, String beforeRef, String afterRef) {
        auditEventRepository.save(new AuditEvent(
                UUID.randomUUID(), tenantId, ActorContext.get(), action, "stand_assignment:" + assignmentId,
                beforeRef, afterRef, null, Instant.now()));
    }
}
