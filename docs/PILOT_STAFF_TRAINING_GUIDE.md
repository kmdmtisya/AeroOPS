# AeroOps — Pilot Staff Training Guide

**Purpose:** support roadmap step 5.7 ("train pilot staff") ahead of Gate G3 sign-off and Phase 6 (Shadow pilot). This is training *material* — a guide for whoever runs the session — not a substitute for the training session itself, which needs to happen with real pilot staff before shadow mode starts.

**Audience:** AOCC controllers, ground-handler supervisors/agents, stand planners, tenant admins, and auditors at the pilot airport — i.e., holders of the `controller`, `handler`, `planner`, `tenant-admin`, `viewer`, and `auditor` realm roles.

**Format suggestion:** one session per role cluster (controllers+planners together, handlers separately, since their day-to-day screens differ), 45–60 minutes each, followed by the fallback drill in `FALLBACK_RUNBOOK.md` §4 as a separate hands-on session.

## 1. What AeroOps is, and is not

- **Is:** an advisory operational dashboard showing flight status, turnarounds, incidents, and stand assignments for your airport's tenant only, plus explainable delay-risk flags.
- **Is not:** a system of record that replaces your existing AODB/handler systems, a system that takes any autonomous action, or a system that can control aircraft, runways, or safety-critical anything (explicitly out of scope per the project roadmap).
- **Every alert and risk flag is advisory only.** A human always decides what to do; AeroOps never assigns a stand, resolves an incident, or completes a task on its own.

## 2. Roles and what you'll see

| Role | Can view | Can do |
|---|---|---|
| `viewer` | All flights, turnarounds, incidents, stands (own tenant only) | Read-only |
| `auditor` | Same as viewer, positioned for access/history review | Read-only |
| `handler` | Same views | Complete turnaround tasks |
| `controller` | Same views | Create/manage incidents, assign stands, create turnarounds, complete tasks |
| `planner` | Same views | Same as controller |
| `tenant-admin` | Same views | Same as controller/planner |

You will only ever see data for your own airport — this is enforced by the platform, not by convention (every request is scoped to the tenant claim in your login token; there is no way, through the UI or otherwise, to view another tenant's data). If you ever see or suspect you're seeing another airport's data, stop and report it immediately — that would be a serious defect, not an expected mode of operation.

## 3. Reading the flight board

- Each row shows a flight's scheduled/estimated/actual times, separately — never collapsed into a single "best guess" time. This is deliberate: you should always be able to see what was planned vs. what's estimated vs. what's actually happened.
- **The "stale" badge**: if a flight's data hasn't been updated recently, it's marked stale rather than silently showing old data as if it were current. Treat a stale flight's displayed state as unconfirmed — verify through your normal channel before acting on it.
- All times are shown in UTC. If your site's convention is local time, be aware of the offset — this is a deliberate design choice (documented internally as "UTC internally, local on render") to avoid daylight-saving-time rendering bugs; a future release may add a local-time display option, but as of this pilot, times are UTC.

## 4. Delay-risk indicators (new in Phase 5)

- Each flight can show a risk level (Low/Medium/High) with a **specific, itemized reason list** — e.g., "estimated on-block delay exceeds 45 minutes," "2 overdue tasks," "1 open incident," "stand assigned despite conflict."
- **This is explainable and rule-based, not a machine-learning prediction.** Every flag traces to a concrete, visible fact about that flight. If you disagree with a flag, the reason list tells you exactly what triggered it, so you can judge it against what you know.
- **If the underlying flight data is stale, the risk assessment is marked low-confidence**, not hidden and not shown with false confidence. Treat a low-confidence risk flag as informational only — verify the underlying flight data first.
- **Nothing happens automatically because of a risk flag.** No autonomous stand reassignment, no auto-generated incident, no notification sent on your behalf (real alert routing is explicitly not part of this pilot). It's a prompt for you to look, not a decision made for you.

## 5. Incidents, turnarounds, and stand assignments

- **Incidents**: created and managed by controllers/planners/tenant-admins. Each has a status (open/assigned/resolved) and an owner. Assigning or resolving an incident is always a deliberate action by a named person — the system records who did it and when (visible in the audit trail), so treat every action here as attributable to you, not anonymous.
- **Turnarounds & tasks**: handlers complete individual tasks (e.g., baggage, fueling, cleaning) as they finish them in real life — the system doesn't infer completion, you tell it. An overdue task (past its expected time, still pending) is one of the inputs to the risk flag above.
- **Stand assignments**: a stand can be assigned despite a conflict (an "override") — this is allowed because real operations sometimes require it, but it's tracked and it feeds the risk flag, so it's visible rather than silent.

## 6. When something looks wrong

- **Data looks stale or missing** → check for the staleness badge/banner first; if present, this is expected outage behavior, not a bug — see `FALLBACK_RUNBOOK.md` and switch to your manual process for anything time-critical.
- **The dashboard won't load at all, or every action errors** → this is a platform outage, not a data problem — see `FALLBACK_RUNBOOK.md` §1b and declare fallback immediately; don't wait to see if it resolves itself.
- **You see data that looks like it belongs to a different airport** → stop, do not act on it, and report immediately — this is a security concern, not a display bug.
- **You disagree with a risk flag or think an incident/stand state is wrong** → AeroOps is advisory; use your judgment and your site's normal process. Report the disagreement anyway (even if you were right to override it) — this feedback is exactly what the pilot's "alert usefulness" measure is meant to capture (see the pilot scorecard).

## 7. What we're asking you to track during the pilot

Per the pilot scorecard (`AEROOPS_DEVELOPMENT_ROADMAP.md`), your feedback directly feeds two measures:
- **Alert usefulness** — was a risk flag actually actionable, or noise? Flag both — false positives matter as much as true ones.
- **Adoption** — whether the tool fits into your actual workflow. If you find yourself avoiding a screen or a feature, that's useful signal, not a failure on your part.

Use whatever feedback channel the pilot's operational lead sets up (Slack channel, weekly review meeting, etc. — not defined by this document; confirm with your site's AeroOps sponsor).

## 8. Before you start using this live

Every pilot staff member should, before shadow mode begins:
1. Complete this session (or read this guide plus a live walkthrough).
2. Participate in, or at minimum witness, the fallback drill described in `FALLBACK_RUNBOOK.md` §4 — knowing the dashboard exists is not the same as knowing what to do when it doesn't work.
3. Know who their site's AeroOps operational lead/sponsor is for questions and feedback during the pilot.

## What this guide deliberately does not cover

- **Click-by-click UI walkthrough with screenshots** — recommend the trainer do a live screen-share walkthrough using this guide as the talking-point outline, since the UI will continue to evolve and screenshots would go stale.
- **Site-specific manual fallback procedures** — those belong to each airport already and are referenced, not defined, by `FALLBACK_RUNBOOK.md`.
- **Feedback-channel setup** — that's an operational decision for the pilot's sponsor, not something this guide prescribes.
