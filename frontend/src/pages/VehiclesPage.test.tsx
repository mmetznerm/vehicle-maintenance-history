import { render, screen, waitFor } from "@testing-library/react";
import {
  deleteMaintenance,
  deleteVehicle,
  listMaintenances,
  listVehicles,
} from "../services/api";
import { VehiclesPage } from "./VehiclesPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    deleteMaintenance: vi.fn(),
    deleteVehicle: vi.fn(),
    listMaintenances: vi.fn(),
    listVehicles: vi.fn(),
  };
});

const deleteMaintenanceMock = vi.mocked(deleteMaintenance);
const deleteVehicleMock = vi.mocked(deleteVehicle);
const listMaintenancesMock = vi.mocked(listMaintenances);
const listVehiclesMock = vi.mocked(listVehicles);

describe("VehiclesPage", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    deleteMaintenanceMock.mockReset();
    deleteVehicleMock.mockReset();
    listMaintenancesMock.mockReset();
    listVehiclesMock.mockReset();
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
});
