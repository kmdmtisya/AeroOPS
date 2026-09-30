package com.aeroops.turnarounds;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "turnaround")
public class Turnaround {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    protected Turnaround() {
        // JPA
    }

    private Turnaround(UUID id, String tenantId, UUID flightId, String templateName) {
        this.id = id;
        this.tenantId = tenantId;
        this.flightId = flightId;
        this.templateName = templateName;
    }

    public static Turnaround create(String tenantId, UUID flightId, String templateName) {
        return new Turnaround(UUID.randomUUID(), tenantId, flightId, templateName);
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

    public String getTemplateName() {
        return templateName;
    }
}
