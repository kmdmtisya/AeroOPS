# AeroOps

Airport operations intelligence and collaboration platform. See `docs/` for the full
proposal (`AEROOPS_SOLUTION_INTENT.md`, `AEROOPS_PROJECT_PLAN.md`,
`AEROOPS_SYSTEM_DESIGN.md`, `AEROOPS_IMPLEMENTATION_PLAN.md`) and the working
checklist in `docs/AEROOPS_DEVELOPMENT_ROADMAP.md`.

This repository currently implements the **Phase 2 — Platform foundation** walking
skeleton: a tenant-isolated, OIDC-authenticated API backed by PostgreSQL, and a web
client that renders a flight board from it. All data is synthetic. No live feed,
stand/turnaround business logic, or writeback exists yet — see the roadmap for what
comes next.

## Layout

```
/apps/web                 React + TypeScript AOCC web client
/services/api             Spring Boot modular backend
/packages/contracts       JSON Schema event contracts
/infra                    docker-compose.yml, Keycloak realm
/tests/fixtures           synthetic flight fixtures
/docs                     planning docs and roadmap
```

## Run locally

Requires Docker Desktop. No local JDK or Maven install is needed — the API is
built inside its own Docker image.

```sh
docker compose -f infra/docker-compose.yml up --build
```

This starts PostgreSQL (migrated with `services/api/src/main/resources/db/migration`),
Keycloak (realm imported from `infra/keycloak/realm-export.json`), the API on
host port `:8082` (container port 8080 — remapped to avoid clashing with other
local projects), and the web client on `:5173`.

Get a token for the seeded demo user and call the API:

```sh
curl -X POST http://localhost:8081/realms/aeroops/protocol/openid-connect/token \
  -d "client_id=aeroops-web" \
  -d "grant_type=password" \
  -d "username=demo.controller" \
  -d "password=demo"

curl http://localhost:8082/v1/flights -H "Authorization: Bearer <access_token>"
```

The demo user is scoped to tenant `demo-airport`, which is seeded (dev profile
only) with the two synthetic flights in `tests/fixtures/`.

## Run tests

```sh
cd services/api
mvn verify
```

(Requires a local JDK 21 + Maven, or run inside the `maven:3.9-eclipse-temurin-21`
Docker image if none is installed.)
