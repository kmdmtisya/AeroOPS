import { useEffect, useState } from "react";
import { fetchFlights } from "./api";
import type { FlightView } from "./types";

function formatTime(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toISOString().slice(11, 16) + "Z";
}

export default function FlightBoard() {
  const [flights, setFlights] = useState<FlightView[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchFlights()
      .then(setFlights)
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading flights…</p>;
  if (error) return <p role="alert">Could not load flights: {error}</p>;

  return (
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
        {flights.map((flight) => (
          <tr key={flight.id}>
            <td>{flight.carrier}{flight.flightNumber}</td>
            <td>{flight.origin} → {flight.destination}</td>
            <td>{formatTime(flight.scheduledOnBlock)}</td>
            <td>{formatTime(flight.estimatedOnBlock)}</td>
            <td>{formatTime(flight.actualOnBlock)}</td>
            <td>{flight.lifecycle}</td>
            <td>{flight.sourceSystem}</td>
            <td>{formatTime(flight.updatedAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
