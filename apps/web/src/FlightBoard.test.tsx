import { screen, waitFor } from "@testing-library/react";
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest";
import FlightBoard from "./FlightBoard";
import { renderWithProviders } from "./test-utils";
import type { FlightView } from "./types";

const sampleFlight: FlightView = {
  id: "f1",
  carrier: "SA",
  flightNumber: "100",
  serviceDate: "2026-09-30",
  origin: "CPT",
  destination: "JNB",
  scheduledOnBlock: "2026-09-30T10:00:00Z",
  estimatedOnBlock: null,
  actualOnBlock: null,
  scheduledOffBlock: "2026-09-30T11:00:00Z",
  estimatedOffBlock: null,
  actualOffBlock: null,
  lifecycle: "SCHEDULED",
  sourceSystem: "SIMULATED_AOCC",
  updatedAt: "2026-09-30T09:00:00Z",
};

describe("FlightBoard", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify([sampleFlight]), { status: 200 }))
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders the flight list from GET /v1/flights", async () => {
    renderWithProviders(<FlightBoard />);

    await waitFor(() => expect(screen.getByText("SA100")).toBeInTheDocument());
    expect(fetch).toHaveBeenCalledWith("/v1/flights", expect.anything());
    expect(screen.getByText(/CPT/)).toBeInTheDocument();
  });
});
