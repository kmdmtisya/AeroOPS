# AeroOps — Manual Fallback Runbook (Platform/Feed Outage)

**Purpose:** support roadmap step 5.7 ("train pilot staff; rehearse the manual fallback procedure for a platform/feed outage") and the cross-cutting rule "manual fallback must always work: airport teams can revert to established procedures during any outage." This is the procedure to rehearse with real pilot staff before Gate G3 sign-off — the rehearsal itself is a human activity this document supports but cannot substitute for.

**Standing principle:** AeroOps is an advisory display layer. It never becomes the only place operational state exists — the airport's established manual/voice/paper process is always the ground truth of record during any outage. AeroOps failing never blocks an operation; it only means controllers lose a convenience view and fall back to what they did before AeroOps existed.

## 1. What "outage" means here, and how to recognize it

There are three distinct failure modes pilot staff should be able to tell apart, because the correct response differs:

### 1a. Feed outage (the partner/simulated data source stops updating)
- **What it looks like:** flights stop updating; every flight's "stale" badge (`FlightBoard.tsx`) and banner (`FlightDetail.tsx`, `role="alert"`) appears; the age shown keeps growing.
- **Why it's safe:** `SimulatedFeedScheduler.tick()` is a no-op when disabled/unreachable — it does not error, crash, or serve stale data as if it were fresh. Every screen still shows a real `updatedAt`, so staleness is always visible, never hidden.
- **What it does NOT mean:** the AeroOps application itself is down. The dashboard, login, and manual actions (assign/resolve/complete) still work — they're just now operating on data that isn't being refreshed by the feed.

### 1b. Platform outage (AeroOps itself is unreachable)
- **What it looks like:** the dashboard fails to load entirely, or every request errors (not just stale data — no data, no login, connection refused/timeout).
- **Why it's different:** there is nothing AeroOps can show here, including a staleness banner — the application isn't running.

### 1c. Partial/degraded outage (some tenants, some endpoints, or intermittent errors)
- **What it looks like:** some screens work, others don't; errors are intermittent; behavior is inconsistent across users.
- **Handling:** treat as platform outage for any affected function until confirmed otherwise — don't assume partial trust in a system behaving inconsistently.

## 2. Immediate actions on any outage (all three modes)

1. **Declare the fallback.** Whoever notices first (any AOCC role) announces on the existing team channel/radio that AeroOps is degraded or down and manual process is now in effect for [affected function]. Don't wait for IT confirmation to start falling back — the cost of a false alarm is low; the cost of continuing to trust stale/unavailable data is not.
2. **Switch to the established manual process** for whichever function is affected — this runbook does not replace or redefine that process; it only says *when* to invoke it and *how to hand back*. (The airport's own manual/voice/paper procedure — the one used before AeroOps existed — is the reference here, not this document.)
3. **Do not attempt to work around the outage inside AeroOps** (e.g., don't guess at a flight's current state and manually log an action based on a guess) — a stale or wrong AeroOps write during an outage can actively confuse the recovery once the platform is back, since it's mixed in with genuine records.
4. **Log the outage window** (start time noticed, what was affected, who declared it) in whatever incident log the site already uses. This feeds the shadow-mode "daily triage of missing/incorrect events" (roadmap Phase 6, step 2) and the post-pilot evaluation (Phase 7).

## 3. Recovery / hand-back procedure

1. **Confirm the underlying cause is resolved** before resuming reliance on AeroOps — for a feed outage, confirm data is actually updating again (staleness badges clearing, `updatedAt` timestamps advancing), not just that the banner disappeared once.
2. **Reconcile.** Any operational events that happened during the outage window and were tracked manually (paper/radio/voice) should be entered into AeroOps as a record of what happened, clearly distinguishable as backfilled — this keeps AeroOps's own history accurate without pretending it was live at the time. (The exact backfill mechanism is a process decision for the pilot site, not something this codebase currently automates — flag this as a gap if the pilot site needs it formalized before shadow mode.)
3. **Announce recovery** the same way the outage was declared — explicit hand-back, not silent resumption, so everyone re-synchronizes on which process (AeroOps vs. manual) is authoritative right now.
4. **Do not silently resume trusting AeroOps** just because the screen looks normal again — confirm with whoever is running the affected function that they've also switched back.

## 4. What pilot staff should rehearse (concrete drill)

Recommended drill, to run with real pilot staff before Gate G3 sign-off:

1. **Simulate a feed outage**: disable the simulated feed (`aeroops.simulated-feed.enabled=false`, or simply stop the scheduler) while staff are actively using the dashboard. Have them notice the staleness badges/banner without being told in advance, and walk through steps 2–4 above using their own site's manual process.
2. **Simulate a platform outage**: stop the `api` container (or block network access to it) and have staff notice the dashboard is unreachable, declare the outage, and fall back — this proves they know to fall back on "nothing loads," not just on "data looks stale."
3. **Rehearse recovery**: bring the feed/platform back, and have staff walk the reconciliation and hand-back steps, including logging the outage window.
4. **Time it.** How long from outage start to declared fallback, and from recovery to confirmed hand-back? This is useful input to the pilot scorecard's "Availability" measure and to tuning any future alerting.
5. **Debrief and record gaps.** Anything unclear, missing, or contested during the drill should be corrected in this runbook before the real shadow pilot (Phase 6) begins — this document should not be treated as final until it has survived one live rehearsal with real staff.

## 5. What this runbook deliberately does not cover

- **The airport's actual manual/voice/paper procedure itself** — this runbook assumes it already exists and simply defines when/how to invoke it alongside AeroOps; it does not create or replace that procedure.
- **Backfill tooling** — reconciling manually-logged events back into AeroOps after an outage is currently a manual data-entry step, not an automated import. If the pilot site needs this formalized or tooled, that's follow-up work, not something Phase 5 built.
- **Alerting/paging for outages** — this runbook assumes a human notices the outage (staleness badge, failed page load) and self-declares; it does not define automated alerting to on-call staff. Roadmap explicitly deferred real alert routing past Phase 4.
- **Multi-tenant fallback coordination** — if a future pilot spans more than one tenant/airport, coordinating simultaneous fallback across tenants is not addressed here.
