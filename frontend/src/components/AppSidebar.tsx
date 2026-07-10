import { CarIcon, PlusIcon, SettingsIcon } from "./Icons";
import { clearAuthTokens, getCurrentUserDisplayName } from "../services/authStorage";

export function AppSidebar() {
  const userDisplayName = getCurrentUserDisplayName();
  const isVehiclesSection = window.location.pathname.startsWith("/vehicles");

  function handleLogout() {
    clearAuthTokens();
    window.location.assign("/login");
  }

  return (
    <aside className="app-sidebar" aria-label="Navegação principal">
      <a className="sidebar-brand" href="/vehicles">
        AutoLog
      </a>

      <div className="sidebar-user">
        <div>
          <strong>Bem-vindo</strong>
          <span>{userDisplayName}</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <a
          className={`sidebar-link${isVehiclesSection ? " is-active" : ""}`}
          href="/vehicles"
          aria-current={isVehiclesSection ? "page" : undefined}
        >
          <CarIcon aria-hidden />
          <span>Painel</span>
        </a>
        <a className="sidebar-link" href="/vehicles/new">
          <PlusIcon aria-hidden />
          <span>Adicionar veículo</span>
        </a>
        <a className="sidebar-link" href="/settings">
          <SettingsIcon aria-hidden />
          <span>Configurações</span>
        </a>
      </nav>

      <button className="sidebar-logout" type="button" onClick={handleLogout}>
        Sair
      </button>
    </aside>
  );
}
