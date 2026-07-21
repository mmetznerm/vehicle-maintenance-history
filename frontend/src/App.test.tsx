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
    expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();
  });

  it("shows the register page on the register route", () => {
    setPath("/register");

    render(<App />);

    expect(screen.getByRole("button", { name: /create account/i })).toBeInTheDocument();
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
      await screen.findByRole("heading", { name: /no registered vehicles/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /add vehicle/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument();
  });

  it("shows the vehicle creation page when the user is authenticated", () => {
    setPath("/vehicles/new");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    render(<App />);

    expect(screen.getByRole("heading", { name: /vehicle details/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /save/i })).toBeInTheDocument();
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
            color: "Silver",
          }),
          { status: 200 },
        ),
      ),
    );

    render(<App />);

    expect(await screen.findByRole("heading", { name: /edit vehicle/i })).toBeInTheDocument();
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
              color: "Silver",
            }),
            { status: 200 },
          ),
        )
        .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 })),
    );

    render(<App />);

    expect(await screen.findByRole("heading", { name: /honda civic/i })).toBeInTheDocument();
    expect(screen.getByText(/no maintenance records/i)).toBeInTheDocument();
  });

  it("shows the maintenance creation page when the user is authenticated", () => {
    setPath("/vehicles/vehicle-id/maintenances/new");
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    render(<App />);

    expect(screen.getByRole("heading", { name: /add maintenance/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /save/i })).toBeInTheDocument();
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
            description: "Oil change",
            cost: 250,
          }),
          { status: 200 },
        ),
      ),
    );

    render(<App />);

    expect(await screen.findByRole("heading", { name: /edit maintenance/i })).toBeInTheDocument();
    expect(await screen.findByDisplayValue("Oil change")).toBeInTheDocument();
  });
});
