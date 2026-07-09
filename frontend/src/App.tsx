import { useEffect } from "react";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { VehiclesPage } from "./pages/VehiclesPage";
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
    return hasAuthTokens() ? <VehiclesPage /> : <Redirect to="/login" />;
  }

  if ((path === "/login" || path === "/register") && hasAuthTokens()) {
    return <Redirect to="/vehicles" />;
  }

  if (path === "/register") {
    return <RegisterPage />;
  }

  if (path === "/" && hasAuthTokens()) {
    return <Redirect to="/vehicles" />;
  }

  return <LoginPage />;
}
