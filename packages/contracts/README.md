# AeroOps contracts

Shared, source-of-truth schemas for the inbound event model described in `docs/AEROOPS_SYSTEM_DESIGN.md` section 5.

- `event-envelope.schema.json` — the outer envelope every inbound event is wrapped in (identity, tenancy, timing).
- `flight-event.schema.json` — the canonical flight payload for `event_type=flight.update`.

These are not yet wired into a real ingestion pipeline (that lands in roadmap Phase 3 / backlog item F02). They exist now so the shape is fixed before ingestion code is written, per `docs/AEROOPS_DEVELOPMENT_ROADMAP.md` Phase 1.

Stand, turnaround, task and incident event payloads will be added here as those modules are built.
