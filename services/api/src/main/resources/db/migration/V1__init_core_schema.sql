-- Core schema for AeroOps pilot walking skeleton.
-- Scope: AirportTenant + FlightLeg (enough to serve GET /v1/flights) + AuditEvent.
-- See docs/AEROOPS_SYSTEM_DESIGN.md section 4 for the full target data model.

CREATE TABLE airport_tenant (
    id            VARCHAR(64) PRIMARY KEY,
    icao_code     VARCHAR(4),
    iata_code     VARCHAR(3),
    name          VARCHAR(200) NOT NULL,
    timezone      VARCHAR(64)  NOT NULL,
    config_version INTEGER     NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- UUID primary keys are assigned by the application (Hibernate UUID generator),
-- not by a database-side default, so migrations stay portable across Postgres and the H2 test database.
CREATE TABLE flight_leg (
    id                    UUID PRIMARY KEY,
    tenant_id             VARCHAR(64)  NOT NULL REFERENCES airport_tenant(id),
    external_id           VARCHAR(128) NOT NULL,
    source_system         VARCHAR(64)  NOT NULL,
    carrier               VARCHAR(8)   NOT NULL,
    flight_number         VARCHAR(16)  NOT NULL,
    service_date          DATE         NOT NULL,
    origin                VARCHAR(8)   NOT NULL,
    destination           VARCHAR(8)   NOT NULL,
    scheduled_on_block    TIMESTAMPTZ,
    estimated_on_block    TIMESTAMPTZ,
    actual_on_block       TIMESTAMPTZ,
    scheduled_off_block   TIMESTAMPTZ,
    estimated_off_block   TIMESTAMPTZ,
    actual_off_block      TIMESTAMPTZ,
    lifecycle             VARCHAR(32)  NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_flight_leg_source_identity UNIQUE (tenant_id, source_system, external_id)
);

CREATE INDEX ix_flight_leg_tenant_service_date ON flight_leg (tenant_id, service_date);

CREATE TABLE audit_event (
    id            UUID PRIMARY KEY,
    tenant_id     VARCHAR(64) NOT NULL REFERENCES airport_tenant(id),
    actor         VARCHAR(128) NOT NULL,
    action        VARCHAR(64)  NOT NULL,
    target        VARCHAR(256) NOT NULL,
    before_ref    TEXT,
    after_ref     TEXT,
    correlation_id VARCHAR(64),
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_event_tenant_occurred_at ON audit_event (tenant_id, occurred_at);
