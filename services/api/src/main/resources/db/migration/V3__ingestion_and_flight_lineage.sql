-- Phase 3 Step 3.1/3.2: real ingestion columns on the source_event stub table
-- (event_type discriminator, correlation id, raw payload body, rejection reason),
-- plus flight_leg lineage tracking so an out-of-order event (older occurred_at)
-- can be detected and ignored instead of regressing already-applied state.
-- source_event has never been written to, so its new NOT NULL columns are safe
-- to add without a default; flight_leg already has seeded rows, so its new
-- column must stay nullable.

ALTER TABLE source_event ADD COLUMN event_type VARCHAR(64) NOT NULL;
ALTER TABLE source_event ADD COLUMN correlation_id VARCHAR(64);
ALTER TABLE source_event ADD COLUMN payload TEXT NOT NULL;
ALTER TABLE source_event ADD COLUMN reject_reason VARCHAR(500);

ALTER TABLE flight_leg ADD COLUMN last_event_occurred_at TIMESTAMPTZ;
