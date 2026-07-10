import { render, screen } from "@testing-library/react";
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
    deleteMaintenanceMock.mockReset();
    deleteVehicleMock.mockReset();
    listMaintenancesMock.mockReset();
    listVehiclesMock.mockReset();
  });

  it("shows maintenance history and hides details when there is only one vehicle", async () => {
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
    listMaintenancesMock.mockResolvedValue([
      {
        id: "maintenance-id",
        vehicleId: "vehicle-id",
        maintenanceDate: "2023-10-15",
        odometer: 45000,
        description: "Troca de óleo",
        cost: 350,
      },
    ]);

    render(<VehiclesPage />);

    expect(await screen.findByRole("heading", { name: /mitsubishi lancer/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /detalhes/i })).not.toBeInTheDocument();
    expect(await screen.findByText("Troca de óleo")).toBeInTheDocument();
    expect(screen.getByText("R$ 350,00")).toBeInTheDocument();
    expect(listMaintenancesMock).toHaveBeenCalledWith("vehicle-id");
  });

  it("keeps vehicle cards unchanged when there is more than one vehicle", async () => {
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
    expect(screen.queryByText(/histórico de manutenções/i)).not.toBeInTheDocument();
    expect(listMaintenancesMock).not.toHaveBeenCalled();
  });
});
