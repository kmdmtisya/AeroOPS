package com.aeroops.turnarounds;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task")
public class Task {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETE = "COMPLETE";

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "turnaround_id", nullable = false)
    private UUID turnaroundId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "expected_at")
    private Instant expectedAt;

    @Column(nullable = false)
    private String status;

    protected Task() {
        // JPA
    }

    private Task(UUID id, String tenantId, UUID turnaroundId, String name, int sequence) {
        this.id = id;
        this.tenantId = tenantId;
        this.turnaroundId = turnaroundId;
        this.name = name;
        this.sequence = sequence;
        this.status = STATUS_PENDING;
    }

    public static Task create(String tenantId, UUID turnaroundId, String name, int sequence) {
        return new Task(UUID.randomUUID(), tenantId, turnaroundId, name, sequence);
    }

    public void markComplete() {
        this.status = STATUS_COMPLETE;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getTurnaroundId() {
        return turnaroundId;
    }

    public String getName() {
        return name;
    }

    public int getSequence() {
        return sequence;
    }

    public Instant getExpectedAt() {
        return expectedAt;
    }

    public String getStatus() {
        return status;
    }
}
