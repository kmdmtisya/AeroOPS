-- Placeholder tables for modules landing in later roadmap phases (Resources, Turnaround, Exceptions, Integration).
-- Kept minimal on purpose: id + tenant scoping + audit columns only, so the shape is reserved
-- without building business logic ahead of docs/AEROOPS_DEVELOPMENT_ROADMAP.md Phase 3.

CREATE TABLE stand (
    id         UUID PRIMARY KEY,
    tenant_id  VARCHAR(64) NOT NULL REFERENCES airport_tenant(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stand_assignment (
    id         UUID PRIMARY KEY,
    tenant_id  VARCHAR(64) NOT NULL REFERENCES airport_tenant(id),
    flight_id  UUID NOT NULL REFERENCES flight_leg(id),
    stand_id   UUID NOT NULL REFERENCES stand(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE turnaround (
    id         UUID PRIMARY KEY,
    tenant_id  VARCHAR(64) NOT NULL REFERENCES airport_tenant(id),
    flight_id  UUID NOT NULL REFERENCES flight_leg(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE milestone (
    id            UUID PRIMARY KEY,
    tenant_id     VARCHAR(64) NOT NULL REFERENCES airport_tenant(id),
    turnaround_id UUID NOT NULL REFERENCES turnaround(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE task (
    id            UUID PRIMARY KEY,
    tenant_id     VARCHAR(64) NOT NULL REFERENCES airport_tenant(id),
    turnaround_id UUID NOT NULL REFERENCES turnaround(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incident (
    id         UUID PRIMARY KEY,
    tenant_id  VARCHAR(64) NOT NULL REFERENCES airport_tenant(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE source_event (
    id            UUID PRIMARY KEY,
    tenant_id     VARCHAR(64) NOT NULL REFERENCES airport_tenant(id),
    source        VARCHAR(64) NOT NULL,
    external_event_id VARCHAR(128) NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    received_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    occurred_at   TIMESTAMPTZ NOT NULL,
    payload_checksum VARCHAR(128) NOT NULL,
    status        VARCHAR(32) NOT NULL,
    CONSTRAINT uq_source_event_dedupe UNIQUE (tenant_id, source, external_event_id)
);
