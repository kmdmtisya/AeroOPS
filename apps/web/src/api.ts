import type { FlightView } from "./types";

// Real OIDC login lands in roadmap Phase 4 alongside the full AOCC dashboard.
// This dev-only token function exists so the flight board can be exercised
// end-to-end today; it will be replaced, not extended.
function getAccessToken(): string | null {
  return localStorage.getItem("aeroops.devToken");
}

export async function fetchFlights(): Promise<FlightView[]> {
  const token = getAccessToken();
  const response = await fetch("/v1/flights", {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) {
    throw new Error(`Failed to load flights: ${response.status}`);
  }
  return response.json();
}
