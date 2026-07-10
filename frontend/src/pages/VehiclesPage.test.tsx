import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  deleteMaintenance,
  deleteVehicle,
  listMaintenances,
  listVehicles,
  logout,
} from "../services/api";
import { getStoredTokens, saveAuthTokens } from "../services/authStorage";
import { VehiclesPage } from "./VehiclesPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    deleteMaintenance: vi.fn(),
    deleteVehicle: vi.fn(),
    listMaintenances: vi.fn(),
    listVehicles: vi.fn(),
    logout: vi.fn(),
  };
});

const deleteMaintenanceMock = vi.mocked(deleteMaintenance);
const deleteVehicleMock = vi.mocked(deleteVehicle);
const listMaintenancesMock = vi.mocked(listMaintenances);
const listVehiclesMock = vi.mocked(listVehicles);
const logoutMock = vi.mocked(logout);

describe("VehiclesPage", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    deleteMaintenanceMock.mockReset();
    deleteVehicleMock.mockReset();
    listMaintenancesMock.mockReset();
    listVehiclesMock.mockReset();
    logoutMock.mockReset();
  });

  it("redirects to vehicle details when there is only one vehicle", async () => {
    const replaceMock = vi.fn();

    vi.stubGlobal("location", {
      ...window.location,
      replace: replaceMock,
    });
    listVehiclesMock.mockResolvedValue([
      {
        id: "vehicle-id",
        plate: "OGO9035",
        brand: "Mitsubishi",
        model: "Lancer",
        manufactureYear: 2012,
        color: "Preta",
      },
    ]);

    render(<VehiclesPage />);

    await waitFor(() => expect(replaceMock).toHaveBeenCalledWith("/vehicles/vehicle-id"));
    expect(listMaintenancesMock).not.toHaveBeenCalled();
  });

  it("keeps the dashboard when there is more than one vehicle", async () => {
    listVehiclesMock.mockResolvedValue([
      {
        id: "vehicle-one",
        plate: "ABC1234",
        brand: "Honda",
        model: "Civic",
        manufactureYear: 2020,
        color: "Prata",
      },
      {
        id: "vehicle-two",
        plate: "DEF5678",
        brand: "Toyota",
        model: "Corolla",
        manufactureYear: 2021,
        color: "Preta",
      },
    ]);

    render(<VehiclesPage />);

    expect(await screen.findByRole("heading", { name: /honda civic/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /toyota corolla/i })).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: /detalhes/i })).toHaveLength(2);
    expect(screen.queryByText(/hist.rico de manuten..es/i)).not.toBeInTheDocument();
    expect(listMaintenancesMock).not.toHaveBeenCalled();
  });

  it("calls logout, clears tokens, and redirects to login", async () => {
    const user = userEvent.setup();
    const assignMock = vi.fn();

    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });
    vi.stubGlobal("location", {
      ...window.location,
      assign: assignMock,
    });
    listVehiclesMock.mockResolvedValue([]);
    logoutMock.mockResolvedValue(undefined);

    render(<VehiclesPage />);

    await screen.findByRole("heading", { name: /nenhum ve.culo cadastrado/i });
    await user.click(screen.getByRole("button", { name: /sair/i }));

    expect(logoutMock).toHaveBeenCalledWith({ refreshToken: "refresh-token" });
    expect(getStoredTokens()).toBeNull();
    expect(assignMock).toHaveBeenCalledWith("/login");
  });
});
