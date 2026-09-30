// Mirrors com.aeroops.flights.FlightView on the API side.
export interface FlightView {
  id: string;
  carrier: string;
  flightNumber: string;
  serviceDate: string;
  origin: string;
  destination: string;
  scheduledOnBlock: string | null;
  estimatedOnBlock: string | null;
  actualOnBlock: string | null;
  scheduledOffBlock: string | null;
  estimatedOffBlock: string | null;
  actualOffBlock: string | null;
  lifecycle: string;
  sourceSystem: string;
  updatedAt: string;
}

// Mirrors com.aeroops.incidents.IncidentView / CreateIncidentRequest / AssignIncidentRequest.
export type IncidentStatus = "OPEN" | "ASSIGNED" | "RESOLVED";

export interface IncidentView {
  id: string;
  flightLegId: string | null;
  standId: string | null;
  category: string;
  status: IncidentStatus;
  owner: string | null;
  description: string;
  openedAt: string;
  resolvedAt: string | null;
}

export interface CreateIncidentRequest {
  category: string;
  description: string;
  flightLegId: string | null;
  standId: string | null;
}

export interface AssignIncidentRequest {
  owner: string;
}

// Mirrors com.aeroops.stands.StandAssignmentView / AssignStandRequest.
export type StandAssignmentStatus = "ACTIVE" | "CANCELLED";

export interface StandAssignmentView {
  id: string;
  flightLegId: string;
  standId: string;
  windowStart: string;
  windowEnd: string;
  status: StandAssignmentStatus;
  override: boolean;
}

export interface AssignStandRequest {
  flightLegId: string;
  standId: string;
  windowStart: string;
  windowEnd: string;
  override: boolean;
}

export interface StandAssignmentConflict {
  conflicting: StandAssignmentView[];
}

// Mirrors com.aeroops.turnarounds.TurnaroundView / TaskView / TaskRevisionView / CompleteTaskRequest.
export type TaskStatus = "PENDING" | "COMPLETE";

export interface TaskRevisionView {
  actor: string;
  actualAt: string;
  recordedAt: string;
}

export interface TaskView {
  id: string;
  name: string;
  sequence: number;
  status: TaskStatus;
  revisions: TaskRevisionView[];
}

export interface TurnaroundView {
  id: string;
  flightLegId: string;
  templateName: string;
  tasks: TaskView[];
}

export interface CompleteTaskRequest {
  actualAt: string;
}

// Mirrors com.aeroops.risk.RiskView / RiskReason / RiskLevel.
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";

export interface RiskReason {
  field: string;
  thresholdBreached: string;
  actualValue: string;
  explanation: string;
}

export interface RiskView {
  level: RiskLevel;
  reasons: RiskReason[];
  lowConfidence: boolean;
}
