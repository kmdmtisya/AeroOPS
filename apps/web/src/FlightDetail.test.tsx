import { screen, waitFor } from "@testing-library/react";
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest";
import { Route, Routes } from "react-router-dom";
import FlightDetail from "./FlightDetail";
import { renderWithProviders } from "./test-utils";
import type { FlightView, RiskView } from "./types";

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
  updatedAt: new Date().toISOString(),
};

const sampleRisk: RiskView = {
  level: "MEDIUM",
  reasons: [
    {
      field: "onBlock",
      thresholdBreached: "warning",
      actualValue: "20 min",
      explanation: "On-block delay of 20 min meets or exceeds the warning threshold of 15 min",
    },
  ],
  lowConfidence: false,
};

describe("FlightDetail", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL) => {
        const url = input.toString();
        if (url === "/v1/flights/f1") {
          return new Response(JSON.stringify(sampleFlight), { status: 200 });
        }
        if (url === "/v1/flights/f1/risk") {
          return new Response(JSON.stringify(sampleRisk), { status: 200 });
        }
        if (url === "/v1/flights/f1/turnarounds") {
          return new Response(null, { status: 404 });
        }
        if (url === "/v1/incidents") {
          return new Response(JSON.stringify([]), { status: 200 });
        }
        if (url.startsWith("/v1/stands/board")) {
          return new Response(JSON.stringify([]), { status: 200 });
        }
        throw new Error(`Unexpected fetch: ${url}`);
      })
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders flight detail, no turnaround yet, and offers to start one for a controller", async () => {
    renderWithProviders(
      <Routes>
        <Route path="/flights/:id" element={<FlightDetail />} />
      </Routes>,
      { roles: ["controller"], initialEntries: ["/flights/f1"] }
    );

    await waitFor(() => expect(screen.getByText(/SA100/)).toBeInTheDocument());
    expect(screen.getByText("No turnaround started yet.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /start turnaround/i })).toBeInTheDocument();
  });

  it("renders the delay risk panel with its level and reasons", async () => {
    renderWithProviders(
      <Routes>
        <Route path="/flights/:id" element={<FlightDetail />} />
      </Routes>,
      { roles: ["controller"], initialEntries: ["/flights/f1"] }
    );

    await waitFor(() => expect(screen.getByText("MEDIUM")).toBeInTheDocument());
    expect(screen.getByText(/On-block delay of 20 min/)).toBeInTheDocument();
  });
});
