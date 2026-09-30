package com.aeroops.stands;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A human-initiated stand assignment for a flight — never auto-assigned, per the
 * roadmap's explicit rule. {@code override} records that this assignment was force-created
 * despite an overlap with another active assignment for the same stand.
 */
@Entity
@Table(name = "stand_assignment")
public class StandAssignment {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "stand_id", nullable = false)
    private UUID standId;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private boolean override;

    protected StandAssignment() {
        // JPA
    }

    private StandAssignment(UUID id, String tenantId, UUID flightId, UUID standId,
                             Instant windowStart, Instant windowEnd, boolean override) {
        this.id = id;
        this.tenantId = tenantId;
        this.flightId = flightId;
        this.standId = standId;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.status = STATUS_ACTIVE;
        this.override = override;
    }

    public static StandAssignment create(String tenantId, UUID flightId, UUID standId,
                                           Instant windowStart, Instant windowEnd, boolean override) {
        return new StandAssignment(UUID.randomUUID(), tenantId, flightId, standId, windowStart, windowEnd, override);
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getFlightId() {
        return flightId;
    }

    public UUID getStandId() {
        return standId;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public String getStatus() {
        return status;
    }

    public boolean isOverride() {
        return override;
    }
}
