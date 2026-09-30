import { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { completeTask, createTurnaround, fetchFlight, fetchFlightRisk, fetchIncidents, fetchStandBoard, fetchTurnaroundForFlight } from "./api";
import { canCompleteTasks, canMutateIncidentsAndStands, rolesFromAccessToken, useAuth } from "./auth";
import { formatDateTime, formatTime, isStale } from "./format";

export default function FlightDetail() {
  const { id } = useParams<{ id: string }>();
  const flightLegId = id!;
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const queryClient = useQueryClient();

  const flightQuery = useQuery({ queryKey: ["flight", flightLegId], queryFn: () => fetchFlight(flightLegId) });
  const turnaroundQuery = useQuery({
    queryKey: ["turnaround", flightLegId],
    queryFn: () => fetchTurnaroundForFlight(flightLegId),
  });
  const incidentsQuery = useQuery({ queryKey: ["incidents"], queryFn: fetchIncidents });
  const riskQuery = useQuery({ queryKey: ["flight", flightLegId, "risk"], queryFn: () => fetchFlightRisk(flightLegId) });
  const standBoardQuery = useQuery({
    queryKey: ["stands", "board", flightQuery.data?.serviceDate],
    queryFn: () => fetchStandBoard(flightQuery.data!.serviceDate),
    enabled: !!flightQuery.data,
  });

  const createTurnaroundMutation = useMutation({
    mutationFn: () => createTurnaround(flightLegId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["turnaround", flightLegId] }),
  });

  const completeTaskMutation = useMutation({
    mutationFn: (taskId: string) => completeTask(turnaroundQuery.data!.id, taskId, { actualAt: new Date().toISOString() }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["turnaround", flightLegId] }),
  });

  const relatedIncidents = useMemo(
    () => (incidentsQuery.data ?? []).filter((i) => i.flightLegId === flightLegId),
    [incidentsQuery.data, flightLegId]
  );
  const standAssignment = useMemo(
    () => (standBoardQuery.data ?? []).find((a) => a.flightLegId === flightLegId),
    [standBoardQuery.data, flightLegId]
  );

  if (flightQuery.isLoading) return <p>Loading flight…</p>;
  if (flightQuery.error) return <p role="alert">Could not load flight: {(flightQuery.error as Error).message}</p>;
  const flight = flightQuery.data!;
  const canMutate = canMutateIncidentsAndStands(roles);

  return (
    <section>
      <p>
        <Link to="/flights">← Back to flights</Link>
      </p>
      <h2>
        {flight.carrier}
        {flight.flightNumber} — {flight.origin} → {flight.destination}
      </h2>
      {isStale(flight.updatedAt) && (
        <p role="alert" className="stale-banner">
          Data may be stale — last updated {formatDateTime(flight.updatedAt)} from {flight.sourceSystem}.
        </p>
      )}

      <h3>Delay risk</h3>
      {riskQuery.data && (
        <div className={riskQuery.data.lowConfidence ? "stale-banner" : undefined}>
          <p>
            Level: <strong>{riskQuery.data.level}</strong>
            {riskQuery.data.lowConfidence && (
              <span className="stale-badge"> (low confidence — flight data may be stale)</span>
            )}
          </p>
          {riskQuery.data.reasons.length === 0 ? (
            <p>No contributing factors.</p>
          ) : (
            <ul>
              {riskQuery.data.reasons.map((reason, index) => (
                <li key={index}>{reason.explanation}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <table>
        <tbody>
          <tr>
            <th>Service date</th>
            <td>{flight.serviceDate}</td>
          </tr>
          <tr>
            <th>Lifecycle</th>
            <td>{flight.lifecycle}</td>
          </tr>
          <tr>
            <th>Source</th>
            <td>{flight.sourceSystem}</td>
          </tr>
          <tr>
            <th>Scheduled / Estimated / Actual on-block</th>
            <td>
              {formatTime(flight.scheduledOnBlock)} / {formatTime(flight.estimatedOnBlock)} /{" "}
              {formatTime(flight.actualOnBlock)}
            </td>
          </tr>
          <tr>
            <th>Scheduled / Estimated / Actual off-block</th>
            <td>
              {formatTime(flight.scheduledOffBlock)} / {formatTime(flight.estimatedOffBlock)} /{" "}
              {formatTime(flight.actualOffBlock)}
            </td>
          </tr>
          <tr>
            <th>Updated</th>
            <td>{formatDateTime(flight.updatedAt)}</td>
          </tr>
        </tbody>
      </table>

      <h3>Stand assignment</h3>
      {standAssignment ? (
        <p>
          Stand {standAssignment.standId} ({formatTime(standAssignment.windowStart)} –{" "}
          {formatTime(standAssignment.windowEnd)}), status {standAssignment.status}
          {standAssignment.override && " (override)"}
        </p>
      ) : (
        <p>No stand assigned yet.</p>
      )}

      <h3>Turnaround</h3>
      {turnaroundQuery.isLoading && <p>Loading turnaround…</p>}
      {!turnaroundQuery.isLoading && !turnaroundQuery.data && (
        <div>
          <p>No turnaround started yet.</p>
          {canMutate && (
            <button disabled={createTurnaroundMutation.isPending} onClick={() => createTurnaroundMutation.mutate()}>
              Start turnaround
            </button>
          )}
        </div>
      )}
      {turnaroundQuery.data && (
        <table>
          <thead>
            <tr>
              <th>Task</th>
              <th>Status</th>
              <th>Last completed</th>
              {canCompleteTasks(roles) && <th>Action</th>}
            </tr>
          </thead>
          <tbody>
            {turnaroundQuery.data.tasks
              .slice()
              .sort((a, b) => a.sequence - b.sequence)
              .map((task) => {
                const lastRevision = task.revisions[task.revisions.length - 1];
                return (
                  <tr key={task.id}>
                    <td>{task.name}</td>
                    <td>{task.status}</td>
                    <td>
                      {lastRevision ? `${lastRevision.actor} @ ${formatDateTime(lastRevision.actualAt)}` : "—"}
                    </td>
                    {canCompleteTasks(roles) && (
                      <td>
                        {task.status === "PENDING" && (
                          <button
                            disabled={completeTaskMutation.isPending}
                            onClick={() => completeTaskMutation.mutate(task.id)}
                          >
                            Complete
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                );
              })}
          </tbody>
        </table>
      )}

      <h3>Related incidents</h3>
      {relatedIncidents.length === 0 && <p>No incidents for this flight.</p>}
      {relatedIncidents.length > 0 && (
        <ul>
          {relatedIncidents.map((incident) => (
            <li key={incident.id}>
              {incident.category} — {incident.status} — {incident.description}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
