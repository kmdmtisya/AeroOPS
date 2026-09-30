import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchStandBoard } from "./api";
import { formatTime } from "./format";

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

export default function StandBoard() {
  const [date, setDate] = useState(todayIsoDate());
  const { data: assignments, error, isLoading } = useQuery({
    queryKey: ["stands", "board", date],
    queryFn: () => fetchStandBoard(date),
  });

  return (
    <section>
      <h2>Stand board</h2>
      <label>
        Date: <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </label>

      {isLoading && <p>Loading stand board…</p>}
      {error && <p role="alert">Could not load stand board: {(error as Error).message}</p>}

      {assignments && (
        <table>
          <thead>
            <tr>
              <th>Stand</th>
              <th>Flight</th>
              <th>Window</th>
              <th>Status</th>
              <th>Override</th>
            </tr>
          </thead>
          <tbody>
            {assignments.map((a) => (
              <tr key={a.id}>
                <td>{a.standId}</td>
                <td>{a.flightLegId}</td>
                <td>
                  {formatTime(a.windowStart)} – {formatTime(a.windowEnd)}
                </td>
                <td>{a.status}</td>
                <td>{a.override ? "Yes" : "No"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
