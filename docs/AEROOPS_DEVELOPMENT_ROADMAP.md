# AeroOps — Development Roadmap

**Version:** 1.0 | **Date:** 29 September 2026
**Purpose:** Single, actionable phase-by-phase checklist consolidating `AEROOPS_SOLUTION_INTENT.md`, `AEROOPS_PROJECT_PLAN.md`, `AEROOPS_SYSTEM_DESIGN.md` and `AEROOPS_IMPLEMENTATION_PLAN.md`. Use this as the working checklist during development; the four source docs remain the reference for rationale, data model and full detail.

**Horizon:** 24 weeks to a limited, single-airport pilot. Nothing here authorizes live operational writeback, autonomous control, or ATC/safety-critical function — see the source docs' explicit exclusions.

---

## How to read this roadmap

Each phase lists: **Weeks | Goal | Steps | Outputs | Exit gate**. Steps are ordered — later phases assume earlier steps are done. Backlog IDs (F01–F10) map to `AEROOPS_IMPLEMENTATION_PLAN.md` §3.

---

## Phase 0 — Discovery (Weeks 1–2) · Gate G0

**Goal:** Confirm the pilot is real and scoped before writing code.

Steps:
1. Name the pilot airport, airport sponsor, and AOCC product owner.
2. Interview AOCC, stand planning, airline liaison and ground-handler users.
3. Walk through 5–10 representative turnarounds and disruptions with real users.
4. Inventory source systems (AODB, handler, feeds) and who has authority over each field.
5. Identify data owners and legal/security constraints.
6. Capture baseline metrics: turnaround completeness, alert latency, stand-conflict resolution time, user update time.
7. Select two representative flights for later testing: a normal turnaround and a late inbound with a stand conflict.

Outputs: signed scope, stakeholder/owner map, access request, baseline KPI numbers.

Exit gate (G0): sponsor and access confirmed.

---

## Phase 1 — Product & data design (Weeks 3–4) · Gate G1

**Goal:** Freeze contracts before building.

Steps:
1. Write user stories and acceptance examples per persona (controller, stand planner, handler supervisor, ramp agent, airline liaison, ops manager, auditor).
2. Draft the canonical flight/event dictionary (see System Design §4 data model: `FlightLeg`, `Stand`, `StandAssignment`, `Turnaround`, `Milestone`, `Task`, `Incident`, `SourceEvent`, `AuditEvent`).
3. Define source priority and correction rules — which system is authoritative for which field.
4. Validate UX wireframes with AOCC/handler users.
5. Complete an initial threat model and operational-impact assessment.
6. Create synthetic flight/stand/turnaround fixtures for development (unblocks work if live feed access slips).
7. Decide the first live feed and its approved transfer method.

Outputs: user stories, event dictionary, threat model, UX prototype, synthetic fixtures.

Exit gate (G1): workflows and data contracts approved.

---

## Phase 2 — Platform foundation (Weeks 5–7)

**Goal:** Stand up the skeleton everything else builds on.

Steps:
1. Create the repository with this layout (from Implementation Plan §2):
   ```
   /aeroops
     /apps/web                 React AOCC + mobile web
     /services/api             Spring Boot modular backend
     /services/ingest          adapters (can begin in same deployable)
     /packages/contracts       API and event schemas
     /infra                    compose, IaC, deployment templates
     /db/migrations            versioned schema changes
     /docs                     ADRs, data dictionary, runbooks
     /tests/fixtures           synthetic airport scenarios
   ```
2. Set up CI, branch protection, secret scanning.
3. Build the containerized local environment (Docker Compose: API, web, PostgreSQL, identity provider, simulated feed).
4. Write initial PostgreSQL migrations for the core entities.
5. Configure Keycloak realm (OIDC), roles: viewer, handler, controller, planner, tenant admin, auditor.
6. Build the API skeleton with tenant-aware authorization enforced at API and repository layers.
7. Add the audit log (immutable `AuditEvent` per System Design §4).
8. Add telemetry/observability (structured logs, traces, correlation IDs).
9. Define environment promotion path and restore procedure.

Backlog: **F01** (Tenant and OIDC foundation).

Exit gate: demonstrate tenant isolation, authenticated API, repeatable deployment. Unauthorized and cross-tenant requests must be denied and audited.

---

## Phase 3 — Operational core (Weeks 8–11)

**Goal:** The complete flight-to-off-block journey, on synthetic data.

Steps:
1. Implement the inbound event envelope and validated, idempotent ingestion (event_id, tenant_id, source, event_type, schema_version, occurred_at, received_at, correlation_id, payload — System Design §5).
2. Add deduplication via source/event ID; malformed/conflicting input goes to a review queue with reason; replay must be idempotent.
3. Build the flight canonical view: distinguish scheduled/estimated/actual times; show source lineage.
4. Build the stand board: occupancy windows, overlap/conflict detection with compatibility policy, human resolution (no auto-assignment).
5. Build turnaround templates and tasks: configurable, concurrent milestones (scheduled → inbound update → on-block → disembark/unload/service → boarding/loading → ready → off-block), each with expected/estimated/actual time and revision history.
6. Build the incident/exception workflow: assign, escalate, resolve, with ownership and audit history.
7. Demonstrate: one complete synthetic flight end-to-end, duplicate feed replay with no extra side effects, and a resolved stand conflict.

Backlog: **F02** (Source-event intake), **F03** (Flight canonical view), **F04** (Stand board and warnings), **F05** (Turnaround templates/tasks), **F06** (Incident workflow).

Exit gate: end-to-end simulated flight passes; replay produces no duplicate side effects.

---

## Phase 4 — AOCC interface & first integration (Weeks 12–14) · Gate G2

**Goal:** Connect one real feed and give controllers a working dashboard.

Steps:
1. Connect the first approved partner feed in staging via an adapter (map source fields to canonical event model; keep adapter contract + test fixtures per source).
2. Build the AOCC dashboard: flight list/detail, filter, drilldown, stale-source warning, at pilot load.
3. Build mobile web task update screens for ramp agents.
4. Build alert routing/notifications.
5. Compare imported flights/times against source records; expose data freshness (last-updated banners; suppress confident recommendations when stale).
6. Complete stakeholder acceptance testing.
7. Complete security review (broken object authorization, tenant crossing, integration credential scope).

Backlog: **F07** (AOCC dashboard).

Exit gate (G2): integrated staging environment plus security review passed.

---

## Phase 5 — Intelligence & hardening (Weeks 15–18) · Gate G3

**Goal:** Add advisory intelligence, then prove the system is safe to run live.

Steps:
1. Implement explainable, rule-based delay-risk indications with configurable thresholds; show reasons and contributing fields; no autonomous action.
2. If approved after security review, add the optional read-only copilot behind a feature flag:
   - Fixed catalog of read-only tools/metrics only, scoped to the caller's own authorization.
   - Every answer includes sources, timestamps, and freshness/uncertainty.
   - No generated SQL against production, no action execution, per-tenant disable switch.
   - Reject unsupported questions and any instructions embedded in source data (prompt-injection defense).
3. Run load tests at pilot-scale concurrency/throughput.
4. Run failure tests: feed outage, delayed events, duplicate events, DST/local-time rendering, DB recovery, concurrent updates.
5. Run backup/restore tests and rehearse rollback.
6. Run access/security tests: cross-tenant reads/writes must fail (target: zero disclosures).
7. Train pilot staff; rehearse the manual fallback procedure for a platform/feed outage.

Backlog: **F08** (Rule-based risk), **F09** (Optional copilot), **F10** (Ops readiness — restore, rollback, fallback rehearsal signed off).

Exit gate (G3): pilot readiness confirmed, including fallback rehearsal. This gate decides whether live shadow mode is safe to start.

---

## Phase 6 — Shadow pilot (Weeks 19–22)

**Goal:** Run it for real, in shadow mode, on a bounded scope.

Steps:
1. Start shadow mode with a bounded flight set and operating window.
2. Run daily triage of missing/incorrect events.
3. Run weekly KPI and staff feedback review.
4. No automatic operational writeback at any point.
5. Promote any changes through change control (approved change, migration plan, backup, rollback owner, operator notice — Implementation Plan §5).
6. On source outage: show last-good timestamp, stop dependent risk signals, switch to the established manual process.

Outputs: shadow-mode run log, defect list, measured results against the scorecard (see below).

Exit gate: pilot acceptance (agreed operational scenarios pass; runbooks, rollback and training complete; operations owner signs off).

---

## Phase 7 — Evaluation (Weeks 23–24) · Gate G4

**Goal:** Decide what happens next, with evidence.

Steps:
1. Compare baseline (Phase 0) metrics with pilot measurements.
2. Document error rates, limitations and operational incidents.
3. Estimate next-phase cost.
4. Decide go/no-go for additional airports, integrations and predictive ML.

Exit gate (G4): pilot evaluation and next-investment decision.

---

## Pilot scorecard (track throughout, evaluate at Phase 7)

| Measure | Definition | Gate |
|---|---|---|
| Event timeliness | Accepted events displayed within 30s / accepted events | ≥95% |
| Milestone completeness | Required actual milestones recorded / expected by template | Baseline + agreed uplift |
| Source agreement | Sampled flight records matching authoritative source | ≥99% for agreed fields |
| Alert usefulness | Controller-validated actionable alerts / reviewed alerts | Threshold set after shadow week 1 |
| Availability | Service accessible during agreed pilot hours | ≥99.5% |
| Access assurance | Cross-tenant data disclosure in tests | 0 |
| Adoption | Assigned users completing representative workflow | Target set after baseline |

---

## Cross-cutting rules (apply in every phase, not a separate step)

- **Source of truth stays explicit.** AeroOps records source and never silently overrides a correction; the AODB/resource system/designated partner owns its authoritative fields.
- **Humans approve consequential changes.** Alerts and AI output are advisory only.
- **Uncertainty is visible.** Always show planned vs estimated vs actual separately; show last-update time; suppress confident recommendations when a feed is stale.
- **Tenant isolation everywhere.** Enforced in API and repository layers, tested every phase from Phase 2 onward.
- **UTC internally, local on render.**
- **Manual fallback must always work.** Airport teams can revert to established procedures during any outage.
- **Backward-compatible migrations only**, so rollback restores the prior application image cleanly.

## What's explicitly out of scope for this roadmap

ATC or aircraft control; runway/safety-critical commands; autonomous stand assignment or external-system writeback; passenger- or baggage-level identity data; comprehensive baggage/security/cargo/maintenance/IoT modules; native mobile apps; predictive ML that acts autonomously; any claim of formal A-CDM compliance. Each requires a separate business case, interface approval and safety review.

## Immediate next steps (first 10 working days, from today)

1. Name the pilot airport, sponsor and AOCC product owner.
2. Identify AODB/handler source owners; request sample event payloads and interface terms.
3. Select the two representative flights (normal turnaround; late inbound with stand conflict).
4. Get MVP boundaries and the authoritative-system map approved.
5. Hold a design review of the event model, tenant isolation and manual fallback.
6. Create the repository and synthetic data once G0 is approved.
