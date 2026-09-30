package com.aeroops.flights;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "flight_leg")
public class FlightLeg {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @Column(nullable = false)
    private String carrier;

    @Column(name = "flight_number", nullable = false)
    private String flightNumber;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(name = "scheduled_on_block")
    private Instant scheduledOnBlock;

    @Column(name = "estimated_on_block")
    private Instant estimatedOnBlock;

    @Column(name = "actual_on_block")
    private Instant actualOnBlock;

    @Column(name = "scheduled_off_block")
    private Instant scheduledOffBlock;

    @Column(name = "estimated_off_block")
    private Instant estimatedOffBlock;

    @Column(name = "actual_off_block")
    private Instant actualOffBlock;

    @Column(nullable = false)
    private String lifecycle;

    @Version
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * occurred_at of the last ingested event actually applied to this row (as opposed to
     * received_at), used to reject an out-of-order replay without regressing already-applied
     * fields. Null until the first event lands.
     */
    @Column(name = "last_event_occurred_at")
    private Instant lastEventOccurredAt;

    protected FlightLeg() {
        // JPA
    }

    public static FlightLeg createFromEvent(UUID id, String tenantId, String sourceSystem,
                                             FlightEventPayload payload, Instant occurredAt) {
        FlightLeg leg = new FlightLeg();
        leg.id = id;
        leg.tenantId = tenantId;
        leg.sourceSystem = sourceSystem;
        leg.externalId = payload.externalId();
        leg.carrier = payload.carrier();
        leg.flightNumber = payload.flightNumber();
        leg.serviceDate = payload.serviceDate();
        leg.origin = payload.origin();
        leg.destination = payload.destination();
        leg.lifecycle = payload.lifecycle();
        leg.scheduledOnBlock = payload.scheduledOnBlock();
        leg.estimatedOnBlock = payload.estimatedOnBlock();
        leg.actualOnBlock = payload.actualOnBlock();
        leg.scheduledOffBlock = payload.scheduledOffBlock();
        leg.estimatedOffBlock = payload.estimatedOffBlock();
        leg.actualOffBlock = payload.actualOffBlock();
        leg.lastEventOccurredAt = occurredAt;
        return leg;
    }

    /**
     * Applies an incoming event's fields on top of this row, per the roadmap's source-lineage
     * rule: an event older than the last one actually applied is ignored outright (returns
     * false, no fields touched), and within an applied event a field left null in the payload
     * never overwrites an existing value.
     */
    public boolean applyUpdate(FlightEventPayload payload, Instant occurredAt) {
        if (lastEventOccurredAt != null && occurredAt.isBefore(lastEventOccurredAt)) {
            return false;
        }
        if (payload.lifecycle() != null) this.lifecycle = payload.lifecycle();
        if (payload.scheduledOnBlock() != null) this.scheduledOnBlock = payload.scheduledOnBlock();
        if (payload.estimatedOnBlock() != null) this.estimatedOnBlock = payload.estimatedOnBlock();
        if (payload.actualOnBlock() != null) this.actualOnBlock = payload.actualOnBlock();
        if (payload.scheduledOffBlock() != null) this.scheduledOffBlock = payload.scheduledOffBlock();
        if (payload.estimatedOffBlock() != null) this.estimatedOffBlock = payload.estimatedOffBlock();
        if (payload.actualOffBlock() != null) this.actualOffBlock = payload.actualOffBlock();
        this.lastEventOccurredAt = occurredAt;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public Instant getScheduledOnBlock() {
        return scheduledOnBlock;
    }

    public Instant getEstimatedOnBlock() {
        return estimatedOnBlock;
    }

    public Instant getActualOnBlock() {
        return actualOnBlock;
    }

    public Instant getScheduledOffBlock() {
        return scheduledOffBlock;
    }

    public Instant getEstimatedOffBlock() {
        return estimatedOffBlock;
    }

    public Instant getActualOffBlock() {
        return actualOffBlock;
    }

    public String getLifecycle() {
        return lifecycle;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastEventOccurredAt() {
        return lastEventOccurredAt;
    }
}
