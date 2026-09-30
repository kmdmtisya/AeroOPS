package com.aeroops.turnarounds;

import com.aeroops.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
public class TurnaroundController {

    private final TurnaroundService turnaroundService;
    private final TurnaroundRepository turnaroundRepository;
    private final TaskRepository taskRepository;
    private final TaskRevisionRepository taskRevisionRepository;

    public TurnaroundController(TurnaroundService turnaroundService, TurnaroundRepository turnaroundRepository,
                                 TaskRepository taskRepository, TaskRevisionRepository taskRevisionRepository) {
        this.turnaroundService = turnaroundService;
        this.turnaroundRepository = turnaroundRepository;
        this.taskRepository = taskRepository;
        this.taskRevisionRepository = taskRevisionRepository;
    }

    @PostMapping("/v1/flights/{id}/turnarounds")
    public ResponseEntity<TurnaroundView> create(@PathVariable("id") UUID flightLegId) {
        Turnaround turnaround = turnaroundService.createForFlight(flightLegId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toView(turnaround));
    }

    @PostMapping("/v1/turnarounds/{id}/tasks/{taskId}/complete")
    public TurnaroundView complete(@PathVariable("id") UUID turnaroundId, @PathVariable UUID taskId,
                                    @RequestBody CompleteTaskRequest request) {
        turnaroundService.completeTask(turnaroundId, taskId, request.actualAt());
        Turnaround turnaround = turnaroundRepository.findByIdAndTenantId(turnaroundId, TenantContext.get())
                .orElseThrow(() -> new NoSuchElementException("Turnaround not found: " + turnaroundId));
        return toView(turnaround);
    }

    @GetMapping("/v1/turnarounds/{id}")
    public TurnaroundView get(@PathVariable UUID id) {
        Turnaround turnaround = turnaroundRepository.findByIdAndTenantId(id, TenantContext.get())
                .orElseThrow(() -> new NoSuchElementException("Turnaround not found: " + id));
        return toView(turnaround);
    }

    private TurnaroundView toView(Turnaround turnaround) {
        String tenantId = TenantContext.get();
        List<TaskView> taskViews = taskRepository
                .findByTurnaroundIdAndTenantIdOrderBySequenceAsc(turnaround.getId(), tenantId).stream()
                .map(task -> TaskView.from(task,
                        taskRevisionRepository.findByTaskIdAndTenantIdOrderByRecordedAtAsc(task.getId(), tenantId)))
                .toList();
        return TurnaroundView.from(turnaround, taskViews);
    }
}
