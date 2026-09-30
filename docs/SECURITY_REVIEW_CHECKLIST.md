# AeroOps — External Security Review Checklist

**Purpose:** Give an external/independent security reviewer a concrete starting point for the Gate G3 review named in `AEROOPS_DEVELOPMENT_ROADMAP.md` (Phase 5, step 2 gates F09 on this; step 6 requires "access/security tests" as input). This is a checklist and pointer document, not the review itself — it does not substitute for independent human judgment, and it should not be treated as a self-certification.

**Scope:** the platform as committed at `7bc9fed` (Phase 4/5 complete). Synthetic/local data only; no real partner feed, no production deployment yet.

## 1. Authentication & session handling

- [ ] Confirm every non-actuator endpoint requires a valid OIDC JWT — see `SecurityConfig.filterChain()` (`services/api/src/main/java/com/aeroops/config/SecurityConfig.java`): `anyRequest().authenticated()`, stateless sessions, CSRF disabled (justified only because there's no cookie/session auth — confirm this reasoning still holds before any browser-session-based auth is added).
- [ ] Confirm token validation (signature, issuer, expiry) is delegated to Spring's `oauth2ResourceServer(jwt(...))` against the configured `issuer-uri` (`application.yml`) — reviewer should verify the issuer is pinned to the real Keycloak instance in every non-dev environment, not overridable by request.
- [ ] Review `RealmRoleConverter` (`services/api/src/main/java/com/aeroops/config/RealmRoleConverter.java`) — it reads `realm_access.roles` from the JWT rather than Spring's default `scope`/`scp` claim. Confirm this mapping can't be spoofed (i.e., the claim comes only from a signature-verified token, never a client-supplied header).
- [ ] Confirm the Keycloak `aeroops-web` client is `publicClient: true` with no client secret (`infra/keycloak/realm-export.json`) — appropriate for a public SPA, but confirm the reviewer agrees this is the right client type for the actual deployment topology (no server-side secret to leak, but also no client authentication — PKCE should be confirmed enabled at the Keycloak client config level).

## 2. Tenant isolation (roadmap cross-cutting rule: "Tenant isolation everywhere")

- [ ] Review `TenantFilter` (`services/api/src/main/java/com/aeroops/tenancy/TenantFilter.java`) — the single choke point binding `TenantContext` from the JWT's `tenant_id` claim. Confirm: (a) no endpoint accepts a client-supplied tenant id in a path/query/body param that could override this, (b) a missing/blank tenant claim fails closed (`403`, confirmed in code — `response.sendError(SC_FORBIDDEN, ...)`).
- [ ] Confirm every repository method that reads or writes tenant-scoped data is actually called with `TenantContext.get()` as a filter, not just "most of them" — a full audit of `*Repository.java` query methods, not just the ones `CrossTenantAuthorizationTest` happens to cover.
- [ ] Review `CrossTenantAuthorizationTest` (`services/api/src/test/java/com/aeroops/CrossTenantAuthorizationTest.java`) — confirms `404` (not `500`, not a cross-tenant `200`) for 7 endpoints across flights/turnarounds/risk/incidents. Confirm this list is exhaustive against the current endpoint inventory (new endpoints added after Phase 5 need the same proof before shipping).
- [ ] Note the one already-found-and-fixed defect for calibration: `IncidentService.getOwned()` and two `Turnaround*` call sites threw a bare `NoSuchElementException` on tenant mismatch, surfacing as an unmapped `500` rather than `404` — fixed via `ResponseStatusException`. This is exactly the class of defect this review should be hunting for elsewhere.

## 3. Authorization (role-based access)

- [ ] Confirm the `@PreAuthorize` role matrix matches intended business rules. Current state (grep of `@PreAuthorize` across `services/api/src/main/java/com/aeroops/`):
  - `IncidentController` (create/assign/resolve): `CONTROLLER`, `PLANNER`, `TENANT_ADMIN`
  - `StandAssignmentController` (assign): `CONTROLLER`, `PLANNER`, `TENANT_ADMIN`
  - `TurnaroundController` (create): `CONTROLLER`, `PLANNER`, `TENANT_ADMIN`; (task complete): adds `HANDLER`
  - `RiskController`, and all GET endpoints generally: no `@PreAuthorize` — any authenticated, tenant-bound user can read. Confirm the reviewer agrees read access should not be further restricted by role (current design: `viewer`, `auditor` roles exist specifically for this).
- [ ] Confirm `RealmRoleConverter`'s fail-closed behavior (documented in its own Javadoc) — an unmapped/malformed role claim should result in *no* granted authority, never a default-permissive one.
- [ ] Confirm no endpoint exists that both mutates data and lacks a `@PreAuthorize` check (a full inventory of `@PostMapping`/`@PutMapping`/`@PatchMapping`/`@DeleteMapping` vs. `@PreAuthorize` presence, not just the ones listed above).

## 4. Data handling & scope boundaries

- [ ] Confirm no passenger- or baggage-level identity data is modeled or stored anywhere (roadmap's explicit out-of-scope list) — spot check entity classes (`FlightLeg`, `Task`, `Incident`, `StandAssignment`) contain no PII fields.
- [ ] Confirm the system performs no autonomous writeback to any external system (roadmap cross-cutting rule) — all controller mutations are operator-initiated (a human clicking "assign"/"resolve"/"complete"), not scheduler- or AI-triggered. `SimulatedFeedScheduler` only ingests; it does not call out.
- [ ] Confirm the risk indicators (`RiskScorer`) are read-only and advisory: no code path where a `RiskAssessment` triggers an automatic action. (Confirmed by code inspection during Phase 5 — reviewer should independently verify, not take this document's word for it.)
- [ ] Review audit logging (`audit_event` table, `ActorContext`) — confirm mutating actions are attributable to a real `preferred_username` from the JWT, not a generic service account, satisfying basic non-repudiation for an operational system.

## 5. Infrastructure & secrets

- [ ] Confirm `.env`/secret files are gitignored and no real credentials are committed (`infra/keycloak/realm-export.json` should contain only demo/synthetic credentials for the `demo-airport` tenant — confirm this is never reused verbatim for a real deployment).
- [ ] Confirm corporate TLS-inspection CA certs under `services/api/certs/` are excluded from version control (`.gitignore`) and are a local build-environment concern only, not shipped in any artifact.
- [ ] Confirm database credentials, Keycloak issuer URI, etc. are all externalized via environment variables (`application.yml`'s `${DB_URL:...}` style defaults) — defaults are dev-only placeholders, not values safe to run in any shared environment unchanged.
- [ ] Review `infra/scripts/backup-postgres.sh` / `restore-postgres.sh` — confirm dump files (which contain full tenant data) are never written anywhere world-readable or committed (`infra/backups/` is gitignored).

## 6. Prompt-injection / AI-specific review (forward-looking, only if F09 is approved)

F09 (optional read-only copilot) is **not implemented** — this section is scoped to gate its *future* approval, per roadmap step 5.2, and should be re-reviewed against the actual implementation if/when F09 is built:
- [ ] Confirm the eventual copilot design uses a fixed catalog of read-only tools/metrics only (no generated SQL against production).
- [ ] Confirm every answer surfaces sources, timestamps, and freshness/uncertainty (consistent with the existing staleness-banner pattern in `format.ts`/`FlightDetail.tsx`).
- [ ] Confirm a per-tenant disable switch exists and defaults appropriately.
- [ ] Confirm the design explicitly rejects unsupported questions and instructions embedded in source/event data (prompt-injection defense) — this needs adversarial testing once built, not just a design read-through.

## 7. Review output

The reviewer should produce, at minimum:
- A pass/fail per section above, with specific findings (file/line references where applicable).
- Any finding rated high-severity should block Gate G3 sign-off until remediated and re-verified.
- A statement of what was *not* covered (e.g., if the reviewer didn't have access to a real deployment environment, penetration testing, or dependency/CVE scanning) — see below.

## What this checklist deliberately does not cover

- **Dependency/CVE scanning** (e.g. `mvn dependency-check`, `npm audit`) — not run as part of this checklist; recommend the reviewer run one against the actual `pom.xml`/`package-lock.json` at review time, since versions drift.
- **Penetration testing** against a live deployment — this checklist is a code/design review aid, not a substitute for dynamic testing against a running environment with real network exposure.
- **Infrastructure-level review** (container hardening, network segmentation, TLS termination, WAF rules) — out of scope; this repository doesn't define production infrastructure yet.
- **Compliance certification** (e.g., SOC2, ISO 27001, aviation-specific regulatory frameworks) — this checklist supports an internal engineering security review, not a formal compliance audit.
