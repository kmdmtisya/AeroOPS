import type { ReactNode } from "react";
import { AuthProvider as OidcAuthProvider, useAuth as useOidcAuth } from "react-oidc-context";

// Keycloak's KC_HOSTNAME (see infra/docker-compose.yml) pins every token's "iss" claim to
// http://keycloak:8081/realms/aeroops regardless of which host:port a caller actually used to
// reach Keycloak — the same reason the API validates JWTs against that exact issuer string.
// A browser can also reach Keycloak directly via "keycloak:8081" (matching this ISSUER
// exactly) once "keycloak" resolves to 127.0.0.1 — e.g. via a hosts-file entry — which is
// required for Keycloak's own rendered pages (e.g. the login form) to work, since those
// pages' links are generated from KC_HOSTNAME too, not from whatever host:port the browser
// used to get there. Rather than depend on that being set up, we point AUTHORITY_BASE at
// localhost:8081 (always reachable, no hosts-file entry required) and skip oidc-client-ts's
// discovery fetch (which would otherwise resolve to AUTHORITY_BASE's issuer field and diverge
// from the "iss" claim the API actually validates against) by supplying explicit endpoint
// metadata: real, browser-reachable URLs for every endpoint, but the exact issuer string that
// will actually appear in the token's "iss" claim.
const AUTHORITY_BASE = import.meta.env.VITE_OIDC_AUTHORITY_BASE ?? "http://localhost:8081/realms/aeroops";
const ISSUER = import.meta.env.VITE_OIDC_ISSUER ?? "http://keycloak:8081/realms/aeroops";

const oidcConfig = {
  authority: AUTHORITY_BASE,
  client_id: "aeroops-web",
  redirect_uri: window.location.origin,
  post_logout_redirect_uri: window.location.origin,
  response_type: "code",
  scope: "openid profile email",
  metadata: {
    issuer: ISSUER,
    authorization_endpoint: `${AUTHORITY_BASE}/protocol/openid-connect/auth`,
    token_endpoint: `${AUTHORITY_BASE}/protocol/openid-connect/token`,
    end_session_endpoint: `${AUTHORITY_BASE}/protocol/openid-connect/logout`,
    jwks_uri: `${AUTHORITY_BASE}/protocol/openid-connect/certs`,
    userinfo_endpoint: `${AUTHORITY_BASE}/protocol/openid-connect/userinfo`,
  },
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};

export function AuthProvider({ children }: { children: ReactNode }) {
  return <OidcAuthProvider {...oidcConfig}>{children}</OidcAuthProvider>;
}

export const useAuth = useOidcAuth;

/** Decodes the realm_access.roles claim out of a Keycloak access token (no signature check —
 * the API is the actual authority; the frontend only uses this to decide what UI to show). */
export function rolesFromAccessToken(accessToken: string | undefined): string[] {
  if (!accessToken) return [];
  const parts = accessToken.split(".");
  if (parts.length !== 3) return [];
  try {
    const payload = JSON.parse(atob(parts[1].replace(/-/g, "+").replace(/_/g, "/")));
    return payload.realm_access?.roles ?? [];
  } catch {
    return [];
  }
}

const MUTATING_ROLES = ["controller", "planner", "tenant-admin"];

export function canMutateIncidentsAndStands(roles: string[]): boolean {
  return roles.some((role) => MUTATING_ROLES.includes(role));
}

export function canCompleteTasks(roles: string[]): boolean {
  return canMutateIncidentsAndStands(roles) || roles.includes("handler");
}
