import { render, screen } from "@testing-library/react";
import { App } from "./App";
import { saveAuthTokens } from "./services/authStorage";

function setPath(path: string) {
  window.history.pushState({}, "", path);
}

describe("App", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
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

  it("shows the vehicles page when the user is authenticated", async () => {
    setPath("/vehicles");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 })),
    );

    render(<App />);

    expect(
      await screen.findByRole("heading", { name: /nenhum ve.culo cadastrado/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /adicionar ve.culo/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sair/i })).toBeInTheDocument();
  });

  it("shows the vehicle creation page when the user is authenticated", () => {
    setPath("/vehicles/new");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    render(<App />);

    expect(screen.getByRole("heading", { name: /detalhes do ve.culo/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /salvar/i })).toBeInTheDocument();
  });
});
