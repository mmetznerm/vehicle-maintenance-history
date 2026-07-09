import { render, screen } from "@testing-library/react";
import { App } from "./App";
import { saveAuthTokens } from "./services/authStorage";

function setPath(path: string) {
  window.history.pushState({}, "", path);
}

describe("App", () => {
  beforeEach(() => {
    localStorage.clear();
    setPath("/");
  });

  it("shows the login page by default", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "AutoLog" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /entrar/i })).toBeInTheDocument();
  });

  it("shows the register page on the register route", () => {
    setPath("/register");

    render(<App />);

    expect(screen.getByRole("button", { name: /criar conta/i })).toBeInTheDocument();
  });

  it("shows the vehicles placeholder when the user is authenticated", () => {
    setPath("/vehicles");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    render(<App />);

    expect(screen.getByRole("heading", { name: /ve.culos/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sair/i })).toBeInTheDocument();
  });
});
