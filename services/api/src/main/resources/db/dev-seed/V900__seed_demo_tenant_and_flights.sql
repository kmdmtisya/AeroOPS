-- Dev-only seed data, loaded only when the 'dev' Spring profile is active (see application.yml).
-- Mirrors tests/fixtures/*.json so the API and the fixtures describe the same two synthetic flights.

INSERT INTO airport_tenant (id, icao_code, iata_code, name, timezone, config_version)
VALUES ('demo-airport', 'FACT', 'CPT', 'Demo Airport (Cape Town)', 'Africa/Johannesburg', 1);

INSERT INTO flight_leg (
    id, tenant_id, external_id, source_system, carrier, flight_number, service_date,
    origin, destination,
    scheduled_on_block, estimated_on_block, actual_on_block,
    scheduled_off_block, estimated_off_block, actual_off_block,
    lifecycle
) VALUES (
    '11111111-1111-1111-1111-111111111111', 'demo-airport', 'SIM-FL-1001', 'SIMULATOR', 'SA', 'SA1234', '2026-09-29',
    'CPT', 'JNB',
    '2026-09-29T07:00:00Z', '2026-09-29T07:02:00Z', '2026-09-29T07:03:00Z',
    '2026-09-29T07:45:00Z', '2026-09-29T07:48:00Z', NULL,
    'TURNAROUND'
);

INSERT INTO flight_leg (
    id, tenant_id, external_id, source_system, carrier, flight_number, service_date,
    origin, destination,
    scheduled_on_block, estimated_on_block, actual_on_block,
    scheduled_off_block, estimated_off_block, actual_off_block,
    lifecycle
) VALUES (
    '22222222-2222-2222-2222-222222222222', 'demo-airport', 'SIM-FL-1002', 'SIMULATOR', 'SA', 'SA5678', '2026-09-29',
    'DUR', 'JNB',
    '2026-09-29T08:00:00Z', '2026-09-29T08:35:00Z', NULL,
    '2026-09-29T08:45:00Z', '2026-09-29T09:20:00Z', NULL,
    'INBOUND'
);
