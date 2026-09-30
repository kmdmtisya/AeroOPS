# AeroOps — Execution Plan (Phase 5: Intelligence & Hardening)

**Version:** 1.0 | **Date:** 30 September 2026
**Purpose:** Translate `AEROOPS_DEVELOPMENT_ROADMAP.md` Phase 5 (Gate G3, "pilot readiness") into concrete, ordered engineering tasks against the *actual* repository state, the same way `AEROOPS_EXECUTION_PLAN_PHASE4.md` did for Phase 4. That plan and the roadmap remain authoritative for rationale and scope; this is the day-to-day execution checklist for Phase 5.

**Where we are:** Phase 4 is complete (see `AEROOPS_EXECUTION_PLAN_PHASE4.md`) and its exit gate is met at the engineering level. **Steps 5.1, 5.2, 5.3, 5.4, and 5.5 are complete** — see their sections below. The *engineering* half of Gate G3 (rule-based risk indicators, concurrency/failure tests, cross-tenant access proof, backup/restore rehearsal, pilot-scale load check) is met. The optional copilot (F09), external security review, pilot staff training, and a live fallback-procedure rehearsal are **not** claimed as done — see "What this plan deliberately does not cover" below.

**Standing constraint (unchanged since Phase 2–4):** G0/G1 business approval has not happened, and no external security review has been run. Everything below continues on synthetic/local data only.

## Progress checklist

- [x] Step 5.1 — Rule-based, explainable delay-risk indicators (F08)
- [x] Step 5.2 — Concurrency & failure tests
- [x] Step 5.3 — Cross-tenant access/security test
- [x] Step 5.4 — Backup/restore + rollback rehearsal
- [x] Step 5.5 — Load test at pilot scale + Phase 5 exit-gate write-up

---

## Phase 5 — Intelligence & Hardening

### Step 5.1 — Rule-based, explainable delay-risk indicators (F08) ✅ DONE

**Goal:** roadmap step 5.1 verbatim: explainable, rule-based delay-risk indications with configurable thresholds; show reasons and contributing fields; no autonomous action.

- [x] **`com.aeroops.risk` package** — `RiskThresholdsProperties` (`@ConfigurationProperties(prefix = "aeroops.risk-thresholds")`, matching `SimulatedFeedProperties`'s shape), `RiskScorer` (pure, no Spring/DB dependency — takes a `FlightLeg` plus its tasks/incidents/stand assignment, returns a `RiskAssessment`), `RiskLevel` (`LOW`/`MEDIUM`/`HIGH`), `RiskReason` (field, threshold, actual value, explanation), `RiskView` (controller-facing DTO), `RiskController` (`GET /v1/flights/{id}/risk`, tenant-scoped via `TenantContext.get()`, no `@PreAuthorize` restriction — read-only like `FlightController`'s GETs). — ✅ _Completed: 2026-09-30_
- [x] **Rules implemented, each independently explainable**: on-block delay vs. scheduled time crossing warning/critical thresholds; overdue pending tasks; open/assigned incidents; forced stand-assignment overrides; a staleness interaction that marks the assessment low-confidence rather than confident when the underlying flight data is stale. — ✅ _Completed: 2026-09-30_
- [x] **Tests** — `RiskScorerTest` (plain JUnit, 12 tests, each rule exercised independently and in combination) and `RiskControllerTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` + `jwt()`, 2 tests, proving tenant-scoping and wiring). — ✅ _Completed: 2026-09-30_
- [x] **Run it** — part of the full-suite run recorded under Step 5.2/5.3 below (`RiskScorerTest` 12/12, `RiskControllerTest` 2/2, both passing). — ✅ _Completed: 2026-09-30_

### Step 5.2 — Concurrency & failure tests ✅ DONE

**Goal:** roadmap step 5.4's failure-test list, scoped to the two genuine coverage gaps plus one confirmation.

- [x] **Optimistic-lock race test** — `FlightLegConcurrencyTest` (`@SpringBootTest`): two threads, synchronized via `ExecutorService` + `CountDownLatch` so both read the same `FlightLeg` row before either writes, then both attempt to save. Asserts exactly one save succeeds and the other throws `ObjectOptimisticLockingFailureException` — proving the existing `@Version` field genuinely protects against lost updates. Required exposing `FlightLeg.getVersion()` and `TenantContext.isBound()` (both small additive changes, no behavior change to existing callers). — ✅ _Completed: 2026-09-30_
- [x] **Feed-outage guard test** — confirmed via `SimulatedFeedSchedulerTest` that `SimulatedFeedScheduler.tick()` is a genuine no-op when `SimulatedFeedProperties.enabled == false`: an outage simply stops producing updates (surfaced as staleness per Phase 4's banner) rather than erroring. — ✅ _Completed: 2026-09-30_
- [x] **DST/UTC-rendering confirmation** — `apps/web/src/format.test.ts`: `formatTime`/`formatDateTime`/`isStale` asserted stable across `process.env.TZ` flips (`UTC`, `America/New_York`, `Europe/London`, `Pacific/Auckland`) spanning both a US and an EU spring-forward DST transition. Confirms by test that always rendering in UTC (never local wall-clock time) structurally avoids the DST-rendering bug class rather than needing a fix. — ✅ _Completed: 2026-09-30_
- [x] **Run it** — Dockerized `mvn test` (cert-import loop + `-B test`): 39 tests, 0 failures, 0 errors, `BUILD SUCCESS`, confirming Step 5.1's and 5.2's backend additions together with zero regressions to Phase 2–4's existing 22 tests. `npx vitest run` for the frontend DST test, passing. — ✅ _Completed: 2026-09-30_

### Step 5.3 — Cross-tenant access/security test ✅ DONE

**Goal:** roadmap step 5.6, "cross-tenant reads/writes must fail (target: zero disclosures)" — the one clean gap in prior coverage: existing tests proved tenant-scoped repository queries don't leak, and role-based authorization within one tenant, but nothing proved the full HTTP path when tenant A's token targets tenant B's actual resource ID.

- [x] **`CrossTenantAuthorizationTest`** (`@SpringBootTest` + `@AutoConfigureMockMvc` + `jwt()`, matching `IncidentAuthorizationTest`'s shape — the first test in this codebase to combine two-tenant seeding with the full HTTP/MockMvc/JWT harness) — seeds two real tenants, then issues requests using tenant A's JWT claims against tenant B's real resource IDs for `GET /v1/flights/{id}`, `GET /v1/flights/{id}/turnarounds`, `GET /v1/flights/{id}/risk`, and the two incident-mutation endpoints (`POST /v1/incidents/{id}/assign`, `.../resolve`), asserting `404` and — for the mutation endpoints — that the target incident's `status`/`owner` are unchanged (proving "no leak" also means "no partial mutation," not just the right status code). — ✅ _Completed: 2026-09-30_
- [x] **Defect found and fixed**: `IncidentService.getOwned()` — used by both `assign()` and `resolve()` — threw a bare, unmapped `java.util.NoSuchElementException` on a tenant-mismatched lookup. With zero `@ControllerAdvice`/`@ExceptionHandler` anywhere in this codebase, that exception propagated unhandled through the full filter chain, surfacing as an HTTP `500` rather than the intended `404` (confirmed live in the first test run: `CrossTenantAuthorizationTest`'s two incident-mutation tests failed with exactly this error). Fixed by replacing the thrown exception with Spring's `ResponseStatusException(HttpStatus.NOT_FOUND, ...)`, which Spring MVC maps automatically — the minimal, convention-consistent fix given no existing global exception-handling infrastructure. The identical bare-exception pattern was found by inspection in `TurnaroundController.get()`, `TurnaroundController.complete()`, and `TurnaroundService.completeTask()` (same shape, not yet covered by any test) and fixed the same way pre-emptively, closing the whole class of defect rather than just the one instance the test happened to name. — ✅ _Completed: 2026-09-30_
- [x] **Run it** — after the fix, `CrossTenantAuthorizationTest`: 5/5 passing. Full suite re-run: 44 tests, 0 failures, 0 errors, `BUILD SUCCESS` — zero regressions. — ✅ _Completed: 2026-09-30_
- [x] **Live-verify against the running Docker Compose stack** — rebuilt and restarted the `api` container on the fixed code. Fetched a real Keycloak token for `demo-airport`'s `demo.controller`. Only one tenant exists in the live dev database (confirmed via `psql`), so per this plan's stated fallback, a well-formed but non-existent UUID stood in as the "not visible to this tenant" resource id. Confirmed `404` (not `500`, not a leak) against all seven tenant-scoped endpoints: `GET /v1/flights/{id}`, `.../turnarounds`, `.../risk`, `GET /v1/turnarounds/{id}`, `POST /v1/incidents/{id}/assign`, `.../resolve`, `POST /v1/turnarounds/{id}/tasks/{taskId}/complete` — the last three confirming the pre-emptive turnaround fix live, not just in tests. — ✅ _Completed: 2026-09-30_

### Step 5.4 — Backup/restore + rollback rehearsal ✅ DONE

**Goal:** roadmap step 5.5, "run backup/restore tests and rehearse rollback." No tooling existed before this step; the test profile's H2 in-memory database can't stand in for this — it requires the real Dockerized Postgres.

- [x] **`infra/scripts/backup-postgres.sh`** — wraps `docker exec infra-postgres-1 pg_dump -Fc` to a local timestamped file under `infra/backups/` (gitignored — dump files are scratch artifacts, never committed). — ✅ _Completed: 2026-09-30_
- [x] **`infra/scripts/restore-postgres.sh`** — wraps `pg_restore --clean --if-exists`, defaulting its target container to a throwaway name (`aeroops-restore-rehearsal`) rather than the live one, so running it without an explicit override cannot touch real dev data by accident. — ✅ _Completed: 2026-09-30_
- [x] **Live rehearsal, performed against a disposable container, never the live dev database**: captured row counts for all 8 tables in the running `infra-postgres-1` (`airport_tenant: 1, flight_leg: 6, turnaround: 1, task: 7, task_revision: 7, incident: 3, stand_assignment: 2, audit_event: 13`); ran `backup-postgres.sh` to dump it (125,225 bytes); started a fresh, disposable `postgres:16-alpine` container (`aeroops-restore-rehearsal`, not attached to the real Compose stack); restored the dump into it and confirmed every row count matched exactly, plus spot-checked tenant id and flight external ids matched. Then **simulated damage** in the rehearsal container (deleted all 3 incidents, inserted a bogus tenant row) and restored the *same* dump a second time — confirming the rollback rehearsal: `incident` count back to 3, bogus tenant gone, all 8 tables back to the original counts. Confirmed throughout, via direct `psql` against `infra-postgres-1`, that the live dev database was never touched. Disposable container removed after the rehearsal. — ✅ _Completed: 2026-09-30_

### Step 5.5 — Load test at pilot scale + Phase 5 exit-gate write-up ✅ DONE

**Goal:** roadmap step 5.3, "run load tests at pilot-scale concurrency/throughput," plus this write-up tying 5.1–5.4 together. No load-testing tool was installed (no k6/gatling/jmeter); rather than add a new heavy dependency for a one-time pilot-scale check, reused `curl` + `xargs -P` against the real running stack.

- [x] **Live-verify** — fetched a real Keycloak token for `demo.controller`. Burst 1: 30 concurrent `GET /v1/flights`, 300 total requests — **300/300 returned `200`**, latency min 8ms / avg 13ms / max 52ms. Burst 2: 25 concurrent `GET /v1/flights/{id}/risk`, 250 total requests cycling across the 6 real flight ids in the live database — **250/250 returned `200`**, latency min 9ms / avg 15ms / max 260ms. `docker logs infra-api-1` over the burst window showed zero errors, exceptions, warnings, or timeouts. — ✅ _Completed: 2026-09-30_
- [x] **HikariCP pool size** — confirmed via `application.yml` that the pool size is not explicitly configured, so Spring Boot's default of 10 connections applies. At the burst concurrency tested (30), all requests still completed cleanly and quickly, so the default was not a bottleneck at this scale — flagged here as an explicit, documented consideration for real pilot-scale tuning (concurrent users, not just concurrent requests, and any longer-running queries added later), not something this plan changed without a measured need to. — ✅ _Completed: 2026-09-30_
- [x] **This document** — ties 5.1–5.5 together, per the same structure/discipline as Phase 3/4's plans. — ✅ _Completed: 2026-09-30_

---

## Sequencing and dependencies

**5.1 → 5.2 → 5.3 → 5.4 → 5.5.** 5.1 has no dependency on the others and is done. 5.2 depends only on existing infrastructure (`@Version`, the simulated feed scheduler, the frontend's existing UTC-only rendering) and is done. 5.3 depends on nothing new and is done — it also uncovered and fixed a real defect (unmapped `NoSuchElementException` → `500` on cross-tenant incident mutation, plus the same latent pattern in two turnaround endpoints) rather than merely confirming an assumption. 5.4 is independent engineering (backup/restore tooling) and is done, rehearsed against a disposable Postgres copy. 5.5 depends on 5.1–5.4 existing to have something meaningful to load-test and write up, and is done.

## Gate G3 assessment

The **engineering** pieces of Gate G3 ("pilot readiness") are done:
- Rule-based, explainable delay-risk indicators (F08) — implemented, tested, no autonomous action.
- Concurrency protection — proven via a genuine two-thread optimistic-lock race test, not just code inspection.
- Failure resilience — feed-outage guard confirmed as a clean no-op; DST/UTC rendering confirmed stable by construction.
- Cross-tenant access — proven end-to-end at the HTTP level across seven endpoints, live against the real stack; a real defect found and fixed in the process (not merely a confirmation).
- Backup/restore — tooling built and rehearsed live against a disposable database copy, including a full rollback rehearsal.
- Load at pilot scale — 550 total concurrent requests across two endpoints, zero errors, low-double-digit-millisecond latency; HikariCP's default pool size documented as a forward-looking consideration.

**Not satisfied by this plan, and not claimable by engineering work alone:**
- **F09 (optional read-only copilot)** — the roadmap gates it on "approved after security review," which has not happened. Skipped for the same standing reason mobile-web and real alert routing were skipped in Phase 4.
- **External security review** — requires a human reviewer; not performed here.
- **Pilot staff training** (roadmap step 5.7) — organizational, not engineering.
- **Live fallback-procedure rehearsal** (roadmap step 5.7) — requires real staff walking a real fallback procedure, not code; the backup/restore *mechanics* were rehearsed (Step 5.4), but a fallback-procedure rehearsal with actual operational staff is distinct and out of scope here.

## What this plan deliberately does not cover

Per the roadmap, the optional read-only copilot (F09) is skipped pending security approval that has not happened — the same standing reason applied to mobile-web (Phase 4) and real notifications (Phase 4). External security review, pilot staff training, and a live fallback rehearsal are organizational/human activities this plan cannot substitute code for, and are not claimed as done.
