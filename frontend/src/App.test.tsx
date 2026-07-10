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
    expect(screen.getAllByRole("link", { name: /adicionar ve.culo/i })).toHaveLength(2);
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

  it("shows the vehicle edit page when the user is authenticated", async () => {
    setPath("/vehicles/vehicle-id/edit");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            id: "vehicle-id",
            plate: "ABC1234",
            brand: "Honda",
            model: "Civic",
            manufactureYear: 2020,
            color: "Prata",
          }),
          { status: 200 },
        ),
      ),
    );

    render(<App />);

    expect(await screen.findByRole("heading", { name: /editar ve.culo/i })).toBeInTheDocument();
    expect(await screen.findByDisplayValue("ABC1234")).toBeInTheDocument();
  });

  it("shows the vehicle details page when the user is authenticated", async () => {
    setPath("/vehicles/vehicle-id");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          new Response(
            JSON.stringify({
              id: "vehicle-id",
              plate: "ABC1234",
              brand: "Honda",
              model: "Civic",
              manufactureYear: 2020,
              color: "Prata",
            }),
            { status: 200 },
          ),
        )
        .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 })),
    );

    render(<App />);

    expect(await screen.findByRole("heading", { name: /honda civic/i })).toBeInTheDocument();
    expect(screen.getByText(/nenhuma manuten..o cadastrada/i)).toBeInTheDocument();
  });

  it("shows the maintenance creation page when the user is authenticated", () => {
    setPath("/vehicles/vehicle-id/maintenances/new");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    render(<App />);

    expect(screen.getByRole("heading", { name: /cadastrar manuten..o/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /salvar/i })).toBeInTheDocument();
  });

  it("shows the maintenance edit page when the user is authenticated", async () => {
    setPath("/vehicles/vehicle-id/maintenances/maintenance-id/edit");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            id: "maintenance-id",
            vehicleId: "vehicle-id",
            maintenanceDate: "2026-07-07",
            odometer: 35000,
            description: "Troca de óleo",
            cost: 250,
          }),
          { status: 200 },
        ),
      ),
    );

    render(<App />);

    expect(await screen.findByRole("heading", { name: /editar manuten..o/i })).toBeInTheDocument();
    expect(await screen.findByDisplayValue("Troca de óleo")).toBeInTheDocument();
  });
});
