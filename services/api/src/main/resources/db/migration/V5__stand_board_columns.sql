-- Phase 3 Step 3.3: real columns on the stand/stand_assignment stub tables
-- (V2__stub_future_modules.sql). Both tables have never been written to, so their
-- new NOT NULL columns are safe to add without a default, same reasoning as V3/V4.

ALTER TABLE stand ADD COLUMN code VARCHAR(16) NOT NULL;
ALTER TABLE stand ADD COLUMN terminal VARCHAR(16);

ALTER TABLE stand_assignment ADD COLUMN window_start TIMESTAMPTZ NOT NULL;
ALTER TABLE stand_assignment ADD COLUMN window_end TIMESTAMPTZ NOT NULL;
ALTER TABLE stand_assignment ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE stand_assignment ADD COLUMN override BOOLEAN NOT NULL DEFAULT false;
