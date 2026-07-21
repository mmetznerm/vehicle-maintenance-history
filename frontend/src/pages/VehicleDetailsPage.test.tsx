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
      color: "Silver",
    });
  });

  it("loads vehicle data and maintenance history", async () => {
    listMaintenancesMock.mockResolvedValue([
      {
        id: "maintenance-id",
        vehicleId: "vehicle-id",
        maintenanceDate: "2023-10-15",
        odometer: 45000,
        description: "Oil and filter change",
        cost: 350,
      },
    ]);

    render(<VehicleDetailsPage />);

    expect(screen.getByRole("status")).toHaveTextContent(/loading vehicle details/i);
    expect(await screen.findByRole("heading", { name: /toyota corolla xei/i })).toBeInTheDocument();
    expect(screen.getByText("ABC1234")).toBeInTheDocument();
    expect(screen.getAllByText("45,000 km")).toHaveLength(2);
    expect(screen.getByText("Oil and filter change")).toBeInTheDocument();
    expect(screen.getByText("R$350.00")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "AutoLog" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument();
    expect(getVehicleMock).toHaveBeenCalledWith("vehicle-id");
    expect(listMaintenancesMock).toHaveBeenCalledWith("vehicle-id");
  });

  it("shows an empty state when there are no maintenances", async () => {
    listMaintenancesMock.mockResolvedValue([]);

    render(<VehicleDetailsPage />);

    expect(await screen.findByText(/no maintenance records/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /add maintenance/i })).toHaveAttribute(
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
        description: "Oil change",
        cost: 350,
      },
    ]);
    deleteMaintenanceMock.mockResolvedValue(undefined);
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<VehicleDetailsPage />);

    await screen.findByText("Oil change");
    await user.click(screen.getByRole("button", { name: /delete maintenance oil change/i }));

    expect(deleteMaintenanceMock).toHaveBeenCalledWith("vehicle-id", "maintenance-id");
    expect(await screen.findByText(/no maintenance records/i)).toBeInTheDocument();
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
    await user.click(screen.getByRole("button", { name: /delete/i }));

    expect(deleteVehicleMock).toHaveBeenCalledWith("vehicle-id");
    expect(assignMock).toHaveBeenCalledWith("/vehicles");
  });
});
