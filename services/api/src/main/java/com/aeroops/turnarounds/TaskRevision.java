package com.aeroops.turnarounds;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only completion history for one task — a repeated completion appends a new
 * row rather than overwriting the previous one, mirroring the "never lose a prior
 * actor's entry" rule from Step 3.4.
 */
@Entity
@Table(name = "task_revision")
public class TaskRevision {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private String actor;

    @Column(name = "actual_at", nullable = false)
    private Instant actualAt;

    @Column(name = "recorded_at", insertable = false, updatable = false)
    private Instant recordedAt;

    protected TaskRevision() {
        // JPA
    }

    private TaskRevision(UUID id, String tenantId, UUID taskId, String actor, Instant actualAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.taskId = taskId;
        this.actor = actor;
        this.actualAt = actualAt;
    }

    public static TaskRevision create(String tenantId, UUID taskId, String actor, Instant actualAt) {
        return new TaskRevision(UUID.randomUUID(), tenantId, taskId, actor, actualAt);
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getActor() {
        return actor;
    }

    public Instant getActualAt() {
        return actualAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
