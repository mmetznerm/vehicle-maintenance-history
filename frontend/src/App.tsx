import { useEffect } from "react";
import { LoginPage } from "./pages/LoginPage";
import { MaintenanceCreatePage } from "./pages/MaintenanceCreatePage";
import { MaintenanceEditPage } from "./pages/MaintenanceEditPage";
import { RegisterPage } from "./pages/RegisterPage";
import { VehicleCreatePage } from "./pages/VehicleCreatePage";
import { VehicleDetailsPage } from "./pages/VehicleDetailsPage";
import { VehicleEditPage } from "./pages/VehicleEditPage";
import { VehiclesPage } from "./pages/VehiclesPage";
import { PublicVehicleHistoryPage } from "./pages/PublicVehicleHistoryPage";
import { hasAuthTokens } from "./services/authStorage";

function Redirect({ to }: { to: string }) {
  useEffect(() => {
    window.location.replace(to);
  }, [to]);

  return null;
}

export function App() {
  const path = window.location.pathname;

  if (/^\/history\/[^/]+$/.test(path)) {
    return <PublicVehicleHistoryPage />;
  }

  if (path === "/vehicles") {
    return hasAuthTokens() ? <VehiclesPage /> : <Redirect to="/login" />;
  }

  if (path === "/vehicles/new") {
    return hasAuthTokens() ? <VehicleCreatePage /> : <Redirect to="/login" />;
  }

  if (/^\/vehicles\/[^/]+\/edit$/.test(path)) {
    return hasAuthTokens() ? <VehicleEditPage /> : <Redirect to="/login" />;
  }

  if (/^\/vehicles\/[^/]+\/maintenances\/new$/.test(path)) {
    return hasAuthTokens() ? <MaintenanceCreatePage /> : <Redirect to="/login" />;
  }

  if (/^\/vehicles\/[^/]+\/maintenances\/[^/]+\/edit$/.test(path)) {
    return hasAuthTokens() ? <MaintenanceEditPage /> : <Redirect to="/login" />;
  }

  if (/^\/vehicles\/[^/]+$/.test(path)) {
    return hasAuthTokens() ? <VehicleDetailsPage /> : <Redirect to="/login" />;
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
