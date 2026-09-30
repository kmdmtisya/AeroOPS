package com.aeroops.stands;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "stand")
public class Stand {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String code;

    private String terminal;

    protected Stand() {
        // JPA
    }

    public Stand(UUID id, String tenantId, String code, String terminal) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.terminal = terminal;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getTerminal() {
        return terminal;
    }
}
