import { useEffect } from "react";
import { LoginPage } from "./pages/LoginPage";
import { VehiclesPagePlaceholder } from "./pages/VehiclesPagePlaceholder";
import { hasAuthTokens } from "./services/authStorage";

function Redirect({ to }: { to: string }) {
  useEffect(() => {
    window.location.replace(to);
  }, [to]);

  return null;
}

export function App() {
  const path = window.location.pathname;

  if (path === "/vehicles") {
    return hasAuthTokens() ? <VehiclesPagePlaceholder /> : <Redirect to="/login" />;
  }

  if (path === "/" && hasAuthTokens()) {
    return <Redirect to="/vehicles" />;
  }

  return <LoginPage />;
}
