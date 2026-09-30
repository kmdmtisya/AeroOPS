import { useEffect } from "react";
import { BrowserRouter, Link, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { rolesFromAccessToken, useAuth } from "./auth";
import { setAccessToken } from "./api";
import FlightBoard from "./FlightBoard";
import FlightDetail from "./FlightDetail";
import IncidentList from "./IncidentList";
import StandBoard from "./StandBoard";

function Nav() {
  return (
    <nav className="nav">
      <Link to="/flights">Flights</Link>
      <Link to="/incidents">Incidents</Link>
      <Link to="/stands/board">Stand board</Link>
    </nav>
  );
}

function LoginGate({ children }: { children: React.ReactNode }) {
  const auth = useAuth();
  const location = useLocation();

  useEffect(() => {
    setAccessToken(auth.user?.access_token ?? null);
  }, [auth.user?.access_token]);

  if (auth.isLoading) {
    return <p>Signing in…</p>;
  }

  if (auth.error) {
    return <p role="alert">Sign-in error: {auth.error.message}</p>;
  }

  if (!auth.isAuthenticated) {
    void auth.signinRedirect({ state: { returnTo: location.pathname } });
    return <p>Redirecting to sign in…</p>;
  }

  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <LoginGate>
        <AppShell />
      </LoginGate>
    </BrowserRouter>
  );
}

function AppShell() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);

  return (
    <main>
      <header className="header">
        <h1>AeroOps — AOCC Dashboard</h1>
        <div className="header-user">
          <span>{auth.user?.profile.preferred_username ?? "signed in"}</span>
          {roles.length > 0 && <span className="roles">({roles.join(", ")})</span>}
          <button onClick={() => void auth.removeUser()}>Sign out</button>
        </div>
      </header>
      <Nav />
      <Routes>
        <Route path="/" element={<Navigate to="/flights" replace />} />
        <Route path="/flights" element={<FlightBoard />} />
        <Route path="/flights/:id" element={<FlightDetail />} />
        <Route path="/incidents" element={<IncidentList />} />
        <Route path="/stands/board" element={<StandBoard />} />
      </Routes>
    </main>
  );
}
