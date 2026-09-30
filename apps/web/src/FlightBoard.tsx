import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { fetchFlights } from "./api";
import { formatTime, isStale } from "./format";

export default function FlightBoard() {
  const { data: flights, error, isLoading } = useQuery({
    queryKey: ["flights"],
    queryFn: fetchFlights,
  });
  const [lifecycleFilter, setLifecycleFilter] = useState("");
  const [sourceFilter, setSourceFilter] = useState("");

  const lifecycles = useMemo(
    () => Array.from(new Set((flights ?? []).map((f) => f.lifecycle))).sort(),
    [flights]
  );
  const sources = useMemo(
    () => Array.from(new Set((flights ?? []).map((f) => f.sourceSystem))).sort(),
    [flights]
  );

  const visibleFlights = (flights ?? []).filter(
    (f) => (!lifecycleFilter || f.lifecycle === lifecycleFilter) && (!sourceFilter || f.sourceSystem === sourceFilter)
  );

  if (isLoading) return <p>Loading flights…</p>;
  if (error) return <p role="alert">Could not load flights: {(error as Error).message}</p>;

  return (
    <section>
      <div className="filters">
        <label>
          Lifecycle:{" "}
          <select value={lifecycleFilter} onChange={(e) => setLifecycleFilter(e.target.value)}>
            <option value="">All</option>
            {lifecycles.map((lc) => (
              <option key={lc} value={lc}>
                {lc}
              </option>
            ))}
          </select>
        </label>
        <label>
          Source:{" "}
          <select value={sourceFilter} onChange={(e) => setSourceFilter(e.target.value)}>
            <option value="">All</option>
            {sources.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
      </div>
      <table>
        <thead>
          <tr>
            <th>Flight</th>
            <th>Route</th>
            <th>Sched. on-block</th>
            <th>Est. on-block</th>
            <th>Act. on-block</th>
            <th>Lifecycle</th>
            <th>Source</th>
            <th>Updated</th>
          </tr>
        </thead>
        <tbody>
          {visibleFlights.map((flight) => (
            <tr key={flight.id} className={isStale(flight.updatedAt) ? "stale" : ""}>
              <td>
                <Link to={`/flights/${flight.id}`}>
                  {flight.carrier}
                  {flight.flightNumber}
                </Link>
              </td>
              <td>
                {flight.origin} → {flight.destination}
              </td>
              <td>{formatTime(flight.scheduledOnBlock)}</td>
              <td>{formatTime(flight.estimatedOnBlock)}</td>
              <td>{formatTime(flight.actualOnBlock)}</td>
              <td>{flight.lifecycle}</td>
              <td>{flight.sourceSystem}</td>
              <td>
                {formatTime(flight.updatedAt)}
                {isStale(flight.updatedAt) && <span className="stale-badge"> stale</span>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
