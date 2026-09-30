package com.aeroops.turnarounds;

import com.aeroops.audit.AuditEvent;
import com.aeroops.audit.AuditEventRepository;
import com.aeroops.tenancy.ActorContext;
import com.aeroops.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TurnaroundService {

    private final TurnaroundRepository turnaroundRepository;
    private final TaskRepository taskRepository;
    private final TaskRevisionRepository taskRevisionRepository;
    private final AuditEventRepository auditEventRepository;

    public TurnaroundService(TurnaroundRepository turnaroundRepository, TaskRepository taskRepository,
                              TaskRevisionRepository taskRevisionRepository,
                              AuditEventRepository auditEventRepository) {
        this.turnaroundRepository = turnaroundRepository;
        this.taskRepository = taskRepository;
        this.taskRevisionRepository = taskRevisionRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public Turnaround createForFlight(UUID flightLegId) {
        String tenantId = TenantContext.get();
        Turnaround turnaround = Turnaround.create(tenantId, flightLegId, TurnaroundTemplate.NAME);
        turnaroundRepository.save(turnaround);

        List<String> steps = TurnaroundTemplate.STEPS;
        for (int sequence = 0; sequence < steps.size(); sequence++) {
            taskRepository.save(Task.create(tenantId, turnaround.getId(), steps.get(sequence), sequence));
        }

        audit(tenantId, "TURNAROUND_CREATED", turnaround.getId(), null, TurnaroundTemplate.NAME);
        return turnaround;
    }

    @Transactional
    public Task completeTask(UUID turnaroundId, UUID taskId, Instant actualAt) {
        String tenantId = TenantContext.get();
        turnaroundRepository.findByIdAndTenantId(turnaroundId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turnaround not found: " + turnaroundId));
        Task task = taskRepository.findByIdAndTurnaroundIdAndTenantId(taskId, turnaroundId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        String actor = ActorContext.get();
        taskRevisionRepository.save(TaskRevision.create(tenantId, task.getId(), actor, actualAt));
        task.markComplete();

        audit(tenantId, "TASK_COMPLETED", task.getId(), null, Task.STATUS_COMPLETE);
        return task;
    }

    private void audit(String tenantId, String action, UUID targetId, String beforeRef, String afterRef) {
        auditEventRepository.save(new AuditEvent(
                UUID.randomUUID(), tenantId, ActorContext.get(), action, "turnaround:" + targetId,
                beforeRef, afterRef, null, Instant.now()));
    }
}
