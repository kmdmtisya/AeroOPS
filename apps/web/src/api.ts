import type {
  AssignIncidentRequest,
  AssignStandRequest,
  CompleteTaskRequest,
  CreateIncidentRequest,
  FlightView,
  IncidentView,
  RiskView,
  StandAssignmentConflict,
  StandAssignmentView,
  TurnaroundView,
} from "./types";

export class ApiError extends Error {
  constructor(message: string, public readonly status: number, public readonly body?: unknown) {
    super(message);
  }
}

export class StandConflictError extends ApiError {
  constructor(public readonly conflicting: StandAssignmentView[]) {
    super("Stand assignment conflicts with an existing assignment", 409);
  }
}

let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => undefined);
    throw new ApiError(`${init?.method ?? "GET"} ${path} failed: ${response.status}`, response.status, body);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

export function fetchFlights(): Promise<FlightView[]> {
  return request("/v1/flights");
}

export function fetchFlight(id: string): Promise<FlightView> {
  return request(`/v1/flights/${id}`);
}

export function fetchFlightRisk(flightLegId: string): Promise<RiskView> {
  return request(`/v1/flights/${flightLegId}/risk`);
}

export function fetchIncidents(): Promise<IncidentView[]> {
  return request("/v1/incidents");
}

export function createIncident(body: CreateIncidentRequest): Promise<IncidentView> {
  return request("/v1/incidents", { method: "POST", body: JSON.stringify(body) });
}

export function assignIncident(id: string, body: AssignIncidentRequest): Promise<IncidentView> {
  return request(`/v1/incidents/${id}/assign`, { method: "POST", body: JSON.stringify(body) });
}

export function resolveIncident(id: string): Promise<IncidentView> {
  return request(`/v1/incidents/${id}/resolve`, { method: "POST" });
}

export function fetchStandBoard(date: string): Promise<StandAssignmentView[]> {
  return request(`/v1/stands/board?date=${encodeURIComponent(date)}`);
}

export async function assignStand(body: AssignStandRequest): Promise<StandAssignmentView> {
  try {
    return await request<StandAssignmentView>("/v1/stand-assignments", { method: "POST", body: JSON.stringify(body) });
  } catch (err) {
    if (err instanceof ApiError && err.status === 409) {
      const conflict = err.body as StandAssignmentConflict;
      throw new StandConflictError(conflict.conflicting);
    }
    throw err;
  }
}

export function fetchTurnaroundForFlight(flightLegId: string): Promise<TurnaroundView | null> {
  return request<TurnaroundView>(`/v1/flights/${flightLegId}/turnarounds`).catch((err) => {
    if (err instanceof ApiError && err.status === 404) return null;
    throw err;
  });
}

export function createTurnaround(flightLegId: string): Promise<TurnaroundView> {
  return request(`/v1/flights/${flightLegId}/turnarounds`, { method: "POST" });
}

export function completeTask(turnaroundId: string, taskId: string, body: CompleteTaskRequest): Promise<TurnaroundView> {
  return request(`/v1/turnarounds/${turnaroundId}/tasks/${taskId}/complete`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}
