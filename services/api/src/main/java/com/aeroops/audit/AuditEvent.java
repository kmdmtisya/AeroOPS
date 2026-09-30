package com.aeroops.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable action history record. No mutating endpoints exist yet in this walking
 * skeleton (see roadmap Phase 2 vs Phase 3), so nothing writes to this table today —
 * it is migrated now (V1__init_core_schema.sql) so Phase 3's incident/task/stand
 * mutations have a stable audit target from day one.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String target;

    @Column(name = "before_ref")
    private String beforeRef;

    @Column(name = "after_ref")
    private String afterRef;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
        // JPA
    }

    public AuditEvent(UUID id, String tenantId, String actor, String action, String target,
                       String beforeRef, String afterRef, String correlationId, Instant occurredAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.actor = actor;
        this.action = action;
        this.target = target;
        this.beforeRef = beforeRef;
        this.afterRef = afterRef;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public String getBeforeRef() {
        return beforeRef;
    }

    public String getAfterRef() {
        return afterRef;
    }
}
