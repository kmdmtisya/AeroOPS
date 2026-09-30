export function formatTime(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toISOString().slice(11, 16) + "Z";
}

export function formatDateTime(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toISOString().replace("T", " ").slice(0, 16) + "Z";
}

/** A flight's data is "stale" once its updatedAt is older than this — surfaced as a banner
 * per docs/AEROOPS_EXECUTION_PLAN_PHASE4.md Step 4.4 (roadmap: "suppress confident
 * recommendations when stale"). No backend change needed — sourceSystem/updatedAt already
 * exist on FlightView; this is pure client-side derived state. */
export const STALENESS_THRESHOLD_MS = 5 * 60 * 1000;

export function isStale(updatedAt: string, thresholdMs: number = STALENESS_THRESHOLD_MS): boolean {
  return Date.now() - new Date(updatedAt).getTime() > thresholdMs;
}
