import { describe, it, expect, afterEach } from "vitest";
import { formatDateTime, formatTime, isStale, STALENESS_THRESHOLD_MS } from "./format";

/**
 * Confirms the Phase 5 Step 5.2 exit-gate claim: formatTime/formatDateTime always render
 * in UTC (never the host's local wall-clock time), so they structurally avoid the DST/
 * local-time rendering bug class by construction rather than needing a fix. Proven by
 * flipping process.env.TZ across a spring-forward DST transition in two different zones
 * and asserting the rendered output never changes.
 */
describe("format (DST/UTC rendering)", () => {
  const originalTz = process.env.TZ;

  afterEach(() => {
    process.env.TZ = originalTz;
  });

  const usSpringForwardInstant = "2026-03-08T07:30:00Z"; // 2:30am -> 3:30am EDT transition, US
  const euSpringForwardInstant = "2026-03-29T01:30:00Z"; // 1:30am -> 2:30am BST/CEST transition, EU

  const zones = ["UTC", "America/New_York", "Europe/London", "Pacific/Auckland"];

  it("formatTime renders the same UTC time regardless of host timezone, across a US DST transition", () => {
    const results = zones.map((tz) => {
      process.env.TZ = tz;
      return formatTime(usSpringForwardInstant);
    });

    expect(new Set(results).size).toBe(1);
    expect(results[0]).toBe("07:30Z");
  });

  it("formatTime renders the same UTC time regardless of host timezone, across an EU DST transition", () => {
    const results = zones.map((tz) => {
      process.env.TZ = tz;
      return formatTime(euSpringForwardInstant);
    });

    expect(new Set(results).size).toBe(1);
    expect(results[0]).toBe("01:30Z");
  });

  it("formatDateTime renders the same UTC date/time regardless of host timezone", () => {
    const results = zones.map((tz) => {
      process.env.TZ = tz;
      return formatDateTime(usSpringForwardInstant);
    });

    expect(new Set(results).size).toBe(1);
    expect(results[0]).toBe("2026-03-08 07:30Z");
  });

  it("isStale compares against the same UTC instant regardless of host timezone", () => {
    const recentIso = new Date(Date.now() - 1000).toISOString();
    const staleIso = new Date(Date.now() - STALENESS_THRESHOLD_MS - 1000).toISOString();

    const recentResults = zones.map((tz) => {
      process.env.TZ = tz;
      return isStale(recentIso);
    });
    const staleResults = zones.map((tz) => {
      process.env.TZ = tz;
      return isStale(staleIso);
    });

    expect(recentResults.every((r) => r === false)).toBe(true);
    expect(staleResults.every((r) => r === true)).toBe(true);
  });
});
