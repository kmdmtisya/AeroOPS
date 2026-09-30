import { screen, waitFor } from "@testing-library/react";
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest";
import StandBoard from "./StandBoard";
import { renderWithProviders } from "./test-utils";
import type { StandAssignmentView } from "./types";

const sampleAssignment: StandAssignmentView = {
  id: "a1",
  flightLegId: "f1",
  standId: "A12",
  windowStart: "2026-09-30T10:00:00Z",
  windowEnd: "2026-09-30T11:00:00Z",
  status: "ACTIVE",
  override: false,
};

describe("StandBoard", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify([sampleAssignment]), { status: 200 }))
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders the stand board from GET /v1/stands/board", async () => {
    renderWithProviders(<StandBoard />);

    await waitFor(() => expect(screen.getByText("A12")).toBeInTheDocument());
    expect(fetch).toHaveBeenCalledWith(expect.stringContaining("/v1/stands/board?date="), expect.anything());
  });
});
