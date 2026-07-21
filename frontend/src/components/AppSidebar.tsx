import { useState } from "react";
import { CarIcon } from "./Icons";
import { logout } from "../services/api";
import { clearAuthTokens, getCurrentUserDisplayName, getRefreshToken } from "../services/authStorage";

export function AppSidebar() {
  const userDisplayName = getCurrentUserDisplayName();
  const isVehiclesSection = window.location.pathname.startsWith("/vehicles");
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  async function handleLogout() {
    if (isLoggingOut) {
      return;
    }

    setIsLoggingOut(true);

    try {
      const refreshToken = getRefreshToken();

      if (refreshToken) {
        await logout({ refreshToken });
      }
    } finally {
      clearAuthTokens();
      window.location.assign("/login");
    }
  }

  return (
    <aside className="app-sidebar" aria-label="Main navigation">
      <a className="sidebar-brand" href="/vehicles">
        Vehicle History
      </a>

      <div className="sidebar-user">
        <div>
          <strong>Welcome</strong>
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
          <span>Dashboard</span>
        </a>
      </nav>

      <button className="sidebar-logout" type="button" disabled={isLoggingOut} onClick={handleLogout}>
        {isLoggingOut ? "Signing out..." : "Sign out"}
      </button>
    </aside>
  );
}
