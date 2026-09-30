import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { assignIncident, createIncident, fetchIncidents, resolveIncident } from "./api";
import { canMutateIncidentsAndStands, rolesFromAccessToken, useAuth } from "./auth";
import { formatDateTime } from "./format";

export default function IncidentList() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const canMutate = canMutateIncidentsAndStands(roles);
  const queryClient = useQueryClient();

  const { data: incidents, error, isLoading } = useQuery({ queryKey: ["incidents"], queryFn: fetchIncidents });

  const [category, setCategory] = useState("BAGGAGE_DELAY");
  const [description, setDescription] = useState("");
  const [owners, setOwners] = useState<Record<string, string>>({});

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["incidents"] });

  const createMutation = useMutation({
    mutationFn: () => createIncident({ category, description, flightLegId: null, standId: null }),
    onSuccess: () => {
      setDescription("");
      invalidate();
    },
  });

  const assignMutation = useMutation({
    mutationFn: ({ id, owner }: { id: string; owner: string }) => assignIncident(id, { owner }),
    onSuccess: invalidate,
  });

  const resolveMutation = useMutation({
    mutationFn: (id: string) => resolveIncident(id),
    onSuccess: invalidate,
  });

  if (isLoading) return <p>Loading incidents…</p>;
  if (error) return <p role="alert">Could not load incidents: {(error as Error).message}</p>;

  return (
    <section>
      <h2>Incidents</h2>

      {canMutate && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
        >
          <label>
            Category:{" "}
            <select value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="BAGGAGE_DELAY">Baggage delay</option>
              <option value="STAND_CONFLICT">Stand conflict</option>
              <option value="GROUND_EQUIPMENT">Ground equipment</option>
              <option value="OTHER">Other</option>
            </select>
          </label>{" "}
          <label>
            Description:{" "}
            <input
              required
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Short description"
            />
          </label>{" "}
          <button type="submit" disabled={createMutation.isPending}>
            Report incident
          </button>
        </form>
      )}

      <table>
        <thead>
          <tr>
            <th>Category</th>
            <th>Description</th>
            <th>Status</th>
            <th>Owner</th>
            <th>Opened</th>
            {canMutate && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {(incidents ?? []).map((incident) => (
            <tr key={incident.id}>
              <td>{incident.category}</td>
              <td>{incident.description}</td>
              <td>{incident.status}</td>
              <td>{incident.owner ?? "—"}</td>
              <td>{formatDateTime(incident.openedAt)}</td>
              {canMutate && (
                <td>
                  {incident.status !== "RESOLVED" && (
                    <>
                      <input
                        placeholder="owner"
                        value={owners[incident.id] ?? ""}
                        onChange={(e) => setOwners({ ...owners, [incident.id]: e.target.value })}
                      />
                      <button
                        disabled={!owners[incident.id] || assignMutation.isPending}
                        onClick={() => assignMutation.mutate({ id: incident.id, owner: owners[incident.id] })}
                      >
                        Assign
                      </button>
                      <button disabled={resolveMutation.isPending} onClick={() => resolveMutation.mutate(incident.id)}>
                        Resolve
                      </button>
                    </>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
