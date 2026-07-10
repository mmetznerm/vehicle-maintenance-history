import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  deleteMaintenance,
  deleteVehicle,
  getVehicle,
  listMaintenances,
} from "../services/api";
import { VehicleDetailsPage } from "./VehicleDetailsPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    deleteMaintenance: vi.fn(),
    deleteVehicle: vi.fn(),
    getVehicle: vi.fn(),
    listMaintenances: vi.fn(),
  };
});

const deleteMaintenanceMock = vi.mocked(deleteMaintenance);
const deleteVehicleMock = vi.mocked(deleteVehicle);
const getVehicleMock = vi.mocked(getVehicle);
const listMaintenancesMock = vi.mocked(listMaintenances);

function setPath(path: string) {
  window.history.pushState({}, "", path);
}

describe("VehicleDetailsPage", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    setPath("/vehicles/vehicle-id");

    getVehicleMock.mockResolvedValue({
      id: "vehicle-id",
      plate: "ABC1234",
      brand: "Toyota",
      model: "Corolla XEI",
      manufactureYear: 2021,
      color: "Prata",
    });
  });

  it("loads vehicle data and maintenance history", async () => {
    listMaintenancesMock.mockResolvedValue([
      {
        id: "maintenance-id",
        vehicleId: "vehicle-id",
        maintenanceDate: "2023-10-15",
        odometer: 45000,
        description: "Troca de óleo e filtro",
        cost: 350,
      },
    ]);

    render(<VehicleDetailsPage />);

    expect(screen.getByRole("status")).toHaveTextContent(/carregando detalhes/i);
    expect(await screen.findByRole("heading", { name: /toyota corolla xei/i })).toBeInTheDocument();
    expect(screen.getByText("ABC1234")).toBeInTheDocument();
    expect(screen.getAllByText("45.000 km")).toHaveLength(2);
    expect(screen.getByText("Troca de óleo e filtro")).toBeInTheDocument();
    expect(screen.getByText("R$ 350,00")).toBeInTheDocument();
    expect(getVehicleMock).toHaveBeenCalledWith("vehicle-id");
    expect(listMaintenancesMock).toHaveBeenCalledWith("vehicle-id");
  });

  it("shows an empty state when there are no maintenances", async () => {
    listMaintenancesMock.mockResolvedValue([]);

    render(<VehicleDetailsPage />);

    expect(await screen.findByText(/nenhuma manuten..o cadastrada/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /adicionar manuten..o/i })).toHaveAttribute(
      "href",
      "/vehicles/vehicle-id/maintenances/new",
    );
  });

  it("deletes a maintenance from the history", async () => {
    const user = userEvent.setup();

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
    deleteMaintenanceMock.mockResolvedValue(undefined);
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<VehicleDetailsPage />);

    await screen.findByText("Troca de óleo");
    await user.click(screen.getByRole("button", { name: /excluir manuten..o troca de .leo/i }));

    expect(deleteMaintenanceMock).toHaveBeenCalledWith("vehicle-id", "maintenance-id");
    expect(await screen.findByText(/nenhuma manuten..o cadastrada/i)).toBeInTheDocument();
  });

  it("deletes the vehicle and redirects to the vehicles page", async () => {
    const user = userEvent.setup();
    const assignMock = vi.fn();

    listMaintenancesMock.mockResolvedValue([]);
    deleteVehicleMock.mockResolvedValue(undefined);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.stubGlobal("location", {
      ...window.location,
      assign: assignMock,
    });

    render(<VehicleDetailsPage />);

    await screen.findByRole("heading", { name: /toyota corolla xei/i });
    await user.click(screen.getByRole("button", { name: /excluir/i }));

    expect(deleteVehicleMock).toHaveBeenCalledWith("vehicle-id");
    expect(assignMock).toHaveBeenCalledWith("/vehicles");
  });
});
