import type { ReactElement } from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthContext, type AuthContextProps } from "react-oidc-context";

/** Minimal fake AuthContext value so pages that call useAuth() render without a real
 * Keycloak redirect — smoke tests only need a stable, already-authenticated user. */
export function fakeAuthContextValue(roles: string[] = ["controller"]): Partial<AuthContextProps> {
  const payload = { realm_access: { roles } };
  const fakeAccessToken = `x.${btoa(JSON.stringify(payload))}.x`;
  return {
    isLoading: false,
    isAuthenticated: true,
    error: undefined,
    user: {
      access_token: fakeAccessToken,
      profile: { preferred_username: "demo.test" },
    } as AuthContextProps["user"],
    signinRedirect: async () => undefined,
    removeUser: async () => undefined,
  };
}

export function renderWithProviders(
  ui: ReactElement,
  { roles, initialEntries }: { roles?: string[]; initialEntries?: string[] } = {}
) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthContext.Provider value={fakeAuthContextValue(roles) as AuthContextProps}>
        <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  );
}
