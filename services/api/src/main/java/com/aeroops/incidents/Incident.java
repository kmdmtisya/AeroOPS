package com.aeroops.incidents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An operational exception raised against a flight and/or stand. Behavior lives on the
 * entity (open/assign/resolve) rather than in the service, same pattern as
 * {@link com.aeroops.flights.FlightLeg#applyUpdate}.
 */
@Entity
@Table(name = "incident")
public class Incident {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_RESOLVED = "RESOLVED";

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "flight_leg_id")
    private UUID flightLegId;

    @Column(name = "stand_id")
    private UUID standId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String status;

    private String owner;

    @Column(nullable = false)
    private String description;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Incident() {
        // JPA
    }

    private Incident(UUID id, String tenantId, UUID flightLegId, UUID standId, String category,
                      String description, Instant openedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.flightLegId = flightLegId;
        this.standId = standId;
        this.category = category;
        this.status = STATUS_OPEN;
        this.description = description;
        this.openedAt = openedAt;
    }

    public static Incident open(String tenantId, UUID flightLegId, UUID standId, String category, String description) {
        return new Incident(UUID.randomUUID(), tenantId, flightLegId, standId, category, description, Instant.now());
    }

    public void assign(String owner) {
        this.owner = owner;
        this.status = STATUS_ASSIGNED;
    }

    public void resolve() {
        this.status = STATUS_RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getFlightLegId() {
        return flightLegId;
    }

    public UUID getStandId() {
        return standId;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public String getOwner() {
        return owner;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
