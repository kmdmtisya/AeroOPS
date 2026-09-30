import { screen, waitFor } from "@testing-library/react";
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest";
import IncidentList from "./IncidentList";
import { renderWithProviders } from "./test-utils";
import type { IncidentView } from "./types";

const sampleIncident: IncidentView = {
  id: "i1",
  flightLegId: null,
  standId: null,
  category: "BAGGAGE_DELAY",
  status: "OPEN",
  owner: null,
  description: "Late bags",
  openedAt: "2026-09-30T09:00:00Z",
  resolvedAt: null,
};

describe("IncidentList", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify([sampleIncident]), { status: 200 }))
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders incidents from GET /v1/incidents and shows mutating controls for a controller role", async () => {
    renderWithProviders(<IncidentList />, { roles: ["controller"] });

    await waitFor(() => expect(screen.getByText("Late bags")).toBeInTheDocument());
    expect(fetch).toHaveBeenCalledWith("/v1/incidents", expect.anything());
    expect(screen.getByRole("button", { name: /report incident/i })).toBeInTheDocument();
  });

  it("hides mutating controls for a viewer role", async () => {
    renderWithProviders(<IncidentList />, { roles: ["viewer"] });

    await waitFor(() => expect(screen.getByText("Late bags")).toBeInTheDocument());
    expect(screen.queryByRole("button", { name: /report incident/i })).not.toBeInTheDocument();
  });
});
