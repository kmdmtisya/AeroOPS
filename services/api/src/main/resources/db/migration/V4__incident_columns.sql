-- Phase 3 Step 3.5: real columns on the incident stub table (V2__stub_future_modules.sql).
-- incident has never been written to, so its new NOT NULL columns are safe to add
-- without a default, same reasoning as V3's source_event columns.

ALTER TABLE incident ADD COLUMN flight_leg_id UUID REFERENCES flight_leg(id);
ALTER TABLE incident ADD COLUMN stand_id UUID REFERENCES stand(id);
ALTER TABLE incident ADD COLUMN category VARCHAR(64) NOT NULL;
ALTER TABLE incident ADD COLUMN status VARCHAR(32) NOT NULL;
ALTER TABLE incident ADD COLUMN owner VARCHAR(128);
ALTER TABLE incident ADD COLUMN description TEXT NOT NULL;
ALTER TABLE incident ADD COLUMN opened_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE incident ADD COLUMN resolved_at TIMESTAMPTZ;
