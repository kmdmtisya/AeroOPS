# AeroOps — Project Plan

**Version:** 1.0 | **Date:** 29 September 2026 | **Planning baseline:** 24 weeks to a limited airport pilot

## 1. Executive brief

AeroOps is a configurable airport operations intelligence and collaboration platform. It ingests approved flight, stand and ground-handling events, tracks turnaround milestones, surfaces conflicts and delay risks, and gives the Airport Operations Control Centre (AOCC) a shared picture. Its first release is advisory: authorized staff approve operational changes in the appropriate system of record.

**Pilot outcome:** At one airport, users can follow a flight from inbound status through stand assignment, turnaround and off-block, trace the provenance of each milestone, resolve exceptions, and measure operational performance. Production integrations depend on each airport's agreements and technical access.

## 2. Scope and success criteria

| In pilot scope | Deferred until after pilot |
|---|---|
| Flight board and flight detail; stand occupancy and conflict warnings; turnaround task templates, updates and timelines; incident/exception workflow; AOCC dashboard; role-based views; event ingestion and replay; notifications; audit trail; read-only, cited operational copilot if approved | Automatic stand allocation or writeback to AODB; ATC instructions; safety-critical control; passenger identity and baggage-level tracking; live airside vehicle control; cross-airport pooled data; autonomous AI decisions; native mobile app |

**Pilot entry:** airport sponsor, operations product owner, data owners, ground-handler partner, approved data-sharing agreements, test environment and sample/approved feeds. **Pilot exit:** agreed operational scenarios pass; data reconciliation and access reviews pass; runbooks, rollback and staff training are complete; operations owner signs off.

Proposed targets, subject to baseline measurement and sponsor agreement: ≥99.5% application availability during pilot operating hours; 95% of accepted events visible within 30 seconds; no duplicate milestone side effects under replay; zero unauthorized cross-tenant reads in security tests; 100% of changes to stands, incidents and milestones audit logged. Measure delay-related outcomes as pilot observations, not a promised causal saving.

## 3. Stakeholders and decisions

| Role | Accountability |
|---|---|
| Airport sponsor / steering group | Funding, pilot airport, operational policy, gate decisions |
| AOCC product owner | Workflows, priority, acceptance and change management |
| Airport data owner / integration lead | Feed contracts, quality rules, source-of-truth mapping |
| Ground-handler lead | Task definitions, update process, operational validation |
| Security/privacy lead | Threat model, access and retention approval |
| Engineering lead | Architecture, release quality, observability |
| Safety assurance lead | Review of operational impacts, fallback and human controls |

**Decision gates:** G0 sponsor and access confirmed (week 2); G1 workflows and data contracts approved (week 4); G2 integrated staging and security review (week 14); G3 pilot readiness including fallback rehearsal (week 18); G4 pilot evaluation and next investment (week 24).

## 4. Work breakdown and schedule

| Phase | Weeks | Outputs | Exit gate |
|---|---:|---|---|
| Discovery | 1–2 | Stakeholder map, process walks, current systems, baseline KPIs, access dependencies | G0 |
| Product/data design | 3–4 | User journeys, event dictionary, acceptance scenarios, threat model, UX prototype | G1 |
| Platform foundation | 5–7 | Repository, CI, local stack, identity, tenant isolation, database migrations, observability | Tenant and audit checks |
| Operational core | 8–11 | Flight ingestion, canonical records, stand board, turnaround milestones, incident workflow | End-to-end simulated flight |
| AOCC and integration | 12–14 | Dashboard, alerts, first approved external feed, replay and reconciliation | G2 |
| Intelligence and hardening | 15–18 | Rule-based delay risks, optional read-only copilot, performance/security tests, runbooks | G3 |
| Pilot | 19–22 | Limited shadow-mode run, user training, defects and measured results | Pilot acceptance |
| Evaluation | 23–24 | KPI comparison, safety/security findings, next-phase roadmap, go/no-go | G4 |

Critical path: partner agreements → feed contract and sample data → source reconciliation → integrated scenarios → security/safety approval → shadow pilot. If live access slips, continue with synthetic data and postpone only the airport-connected pilot gate.

## 5. Team and effort assumptions

Indicative core team: 1 product owner (airport-side), 1 operations SME, 1 tech lead/backend engineer, 1 additional backend/integration engineer, 1 frontend engineer, 1 QA/automation engineer, 0.5 DevOps/SRE, 0.5 UX designer, plus part-time security, data governance and safety reviewers. This is an estimate for planning, not a cost quote. Re-estimate after discovery and vendor/interface review.

## 6. Delivery governance

Two-week iterations; weekly integration/demo review with AOCC and handler; monthly steering gate. Maintain a decision log, prioritized backlog, data contract register, hazard/operational-impact log, risk register and traceability from requirement to acceptance evidence. Use feature flags for pilot features; require peer review and automated CI for migrations and deployments. Assign one owner to each incident and decision.

## 7. Principal risks

| Risk | Impact | Mitigation / trigger |
|---|---|---|
| Delayed AODB or handler access | Pilot cannot use live events | Agree sample feed and contract by G1; simulator and shadow mode; replan G3 if access absent |
| Contradictory timestamps / duplicate events | Wrong turnaround state | Event IDs, source priority, UTC normalization, reconciliation queue and visible data-quality status |
| Stand recommendation mistaken for authority | Operational harm | Advisory labels, explicit approval and no automatic writeback; safety review before changes |
| Multi-tenant data leakage | Privacy and contractual breach | Tenant-scoped authorization, DB isolation policies, tests and independent assessment |
| Low frontline adoption | Stale task data | Handler co-design, fast mobile web input, ownership, training and missing-update alerts |
| AI hallucination or prompt injection | Misleading decision | Tool allowlist, source citations, freshness timestamps, read-only access, evaluation and disable switch |

## 8. Budget model and next decisions

Estimate by people-months, hosting environments, integration/vendor fees, security assessment, training and pilot support; price only after feed access and deployment constraints are known. Select the first airport and two initial partners, define which existing system owns stands and flight records, agree data access, then approve G0.

## 9. Reference basis

This plan uses the EUROCONTROL A-CDM specification and implementation material as a conceptual reference for shared milestones and collaboration; adoption and interface obligations are airport-specific. See the design document's sources and local validation notes.
