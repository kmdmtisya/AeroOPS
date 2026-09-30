# AeroOps — Implementation Plan

**Version:** 1.0 | **Date:** 29 September 2026 | **Execution horizon:** 24 weeks, subject to partner access

## 1. Delivery sequence

**Iteration 0, weeks 1–2 — establish the pilot.** Interview AOCC, stand planning, airline and ground-handler users; walk through 5–10 representative turnarounds and disruptions; inventory source interfaces and authority; identify data owners and legal/security constraints. Capture baseline turnaround completeness, alert latency, stand-conflict resolution and user update time. Deliver a signed scope, owner map and access request.

**Iterations 1–2, weeks 3–4 — freeze contracts.** Produce user stories and acceptance examples; draft canonical flight/event dictionary, source priority and correction rules; validate UX wireframes; complete initial threat and operational-impact assessment. Create synthetic flight/stand/turnaround fixtures for development. Decide first feed and approved transfer method.

**Iterations 3–4, weeks 5–7 — build foundation.** Set up monorepo, CI, branch protection, secret scanning, containerized local environment, PostgreSQL migrations, Keycloak realm/config, API skeleton, audit log, tenant-aware authorization and telemetry. Define environment promotion and restore procedure. Gate: demonstrate tenant isolation, authenticated API and repeatable deployment.

**Iterations 5–6, weeks 8–11 — deliver operational core.** Implement validated idempotent event ingestion and reconciliation; flight list/detail; stand occupancy and conflict rules; turnaround templates and tasks; milestone correction history; incidents and assignment. Demonstrate one complete synthetic flight, duplicate feed replay and a stand conflict.

**Iteration 7, weeks 12–14 — integrate and visualize.** Connect the first approved feed in staging, build AOCC views, mobile web task updates and alert routing. Compare imported flights/times to source records and expose data freshness. Complete stakeholder acceptance and security review.

**Iteration 8, weeks 15–18 — harden and rehearse.** Add explainable rule-based delay risk with threshold configuration. If approved, introduce a read-only copilot using governed tools and source citations, behind a flag. Run load, failure, backup/restore, access, stale-feed and fallback tests. Train pilot staff; rehearse rollback. G3 decides whether live shadow mode is safe.

**Iterations 9–10, weeks 19–22 — pilot.** Start shadow mode with a bounded flight set and operating window. Daily triage of missing/incorrect events; weekly KPI and staff feedback review. No automatic operational writeback. Promote changes through change control.

**Iteration 11, weeks 23–24 — evaluate.** Compare baseline with pilot measurements and error rates, document limitations and operational incidents, estimate next-phase cost and decide go/no-go for additional airports, integrations and predictive ML.

## 2. Initial repository and environments

```text
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

Local environment: Docker Compose with API, web, PostgreSQL, identity provider and simulated feed. Staging: isolated tenant data and approved integration endpoints. Pilot: least-privilege production-like deployment, dedicated secrets and backup policy. Promote the same immutable image between environments; use feature flags and migration compatibility checks.

## 3. Backlog in dependency order

| ID | Story / output | Acceptance evidence |
|---|---|---|
| F01 | Tenant and OIDC foundation | Unauthorized and cross-tenant requests denied, audited |
| F02 | Source-event intake | Valid event accepted once; duplicate replay has no extra effects |
| F03 | Flight canonical view | Scheduled/estimated/actual distinguishable; lineage visible |
| F04 | Stand board and warnings | Overlap detected; manual resolution preserved with actor/time |
| F05 | Turnaround templates/tasks | Concurrent tasks, corrections and SLA status rendered correctly |
| F06 | Incident workflow | Assign, escalate, resolve with history and permissions |
| F07 | AOCC dashboard | List, filter, drilldown and stale-source warning work at pilot load |
| F08 | Rule-based risk | Reasons and contributing fields shown; no autonomous action |
| F09 | Optional copilot | Only authorized read-only answers, cited and freshness stamped |
| F10 | Ops readiness | Restore, rollback and manual fallback rehearsal signed off |

## 4. Test and acceptance strategy

Contract tests for every partner mapping and schema version; unit tests for time, stand overlap, authorization and risk rules; integration tests for transaction/outbox/replay; end-to-end tests for synthetic inbound-to-off-block and correction flows. Security tests include broken object authorization, tenant crossing, integration credentials and AI tool scope. Exercise feed outage, delayed events, duplicate events, DST/local time rendering, database recovery and concurrent updates. Validate dashboard usability with controllers and ramp staff. Do not certify operational safety from software tests alone; airport safety owner reviews process impacts.

## 5. Release and operational controls

Release checklist: approved change, migration plan, latest backup, health/lag baseline, feature-flag plan, operator notice, rollback owner and post-release reconciliation. In pilot, rollback disables new functions and restores prior application image; database migrations must be backward compatible. On source outage show last good timestamp, stop dependent risk signals and switch to established manual process. On detected data breach follow airport incident response and preserve audit evidence.

## 6. Measurable pilot scorecard

| Measure | Definition | Proposed gate |
|---|---|---|
| Event timeliness | Accepted events displayed within 30 s / accepted events | ≥95% |
| Milestone completeness | Required actual milestones recorded / expected by template | Baseline + agreed uplift |
| Source agreement | Sampled flight records matching authoritative source | ≥99% for agreed fields |
| Alert usefulness | Controller-validated actionable alerts / reviewed alerts | Threshold agreed after shadow week 1 |
| Availability | Service accessible during agreed pilot hours | ≥99.5% |
| Access assurance | Cross-tenant data disclosure in tests | 0 |
| Adoption | Assigned users completing representative workflow | Target set after baseline |

## 7. Immediate next 10 working days

1. Name the pilot airport, airport sponsor and AOCC product owner.
2. Identify AODB/resource and handler source owners and obtain sample event payloads plus interface terms.
3. Select two representative flights: normal turnaround and late inbound with stand conflict.
4. Approve MVP boundaries and authoritative-system map.
5. Hold a design review of the event model, tenant isolation and manual fallback.
6. Create the repository and synthetic data after G0 approval.

**Dependencies to resolve before live claims:** partner access, local security policy, safety review, workload sizing and airport-specific A-CDM practice. The schedule is a planning estimate, not a commitment by an airport or provider.
