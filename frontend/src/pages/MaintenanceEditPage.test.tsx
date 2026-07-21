import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiError, getMaintenance, updateMaintenance } from "../services/api";
import { MaintenanceEditPage } from "./MaintenanceEditPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    getMaintenance: vi.fn(),
    updateMaintenance: vi.fn(),
  };
});

const getMaintenanceMock = vi.mocked(getMaintenance);
const updateMaintenanceMock = vi.mocked(updateMaintenance);

function setPath(path: string) {
  window.history.pushState({}, "", path);
}

describe("MaintenanceEditPage", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    getMaintenanceMock.mockReset();
    updateMaintenanceMock.mockReset();
    setPath("/vehicles/vehicle-id/maintenances/maintenance-id/edit");
  });

  it("loads and fills the maintenance form", async () => {
    getMaintenanceMock.mockResolvedValue({
      id: "maintenance-id",
      vehicleId: "vehicle-id",
      maintenanceDate: "2026-07-07",
      odometer: 35000,
      description: "Oil change",
      cost: 250,
    });

    render(<MaintenanceEditPage />);

    expect(screen.getByRole("status")).toHaveTextContent(/loading maintenance/i);
    expect(await screen.findByDisplayValue("2026-07-07")).toBeInTheDocument();
    expect(screen.getByDisplayValue("35000")).toBeInTheDocument();
    expect(screen.getByDisplayValue("250.00")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Oil change")).toBeInTheDocument();
    expect(getMaintenanceMock).toHaveBeenCalledWith("vehicle-id", "maintenance-id");
  });

  it("submits the updated maintenance and redirects to vehicle details", async () => {
    const user = userEvent.setup();
    const assignMock = vi.fn();

    getMaintenanceMock.mockResolvedValue({
      id: "maintenance-id",
      vehicleId: "vehicle-id",
      maintenanceDate: "2026-07-07",
      odometer: 35000,
      description: "Oil change",
      cost: 250,
    });
    updateMaintenanceMock.mockResolvedValue({
      id: "maintenance-id",
      vehicleId: "vehicle-id",
      maintenanceDate: "2026-07-08",
      odometer: 36000,
      description: "Oil and filter change",
      cost: 320,
    });
    vi.stubGlobal("location", {
      ...window.location,
      assign: assignMock,
    });

    render(<MaintenanceEditPage />);

    await screen.findByDisplayValue("Oil change");
    fireEvent.change(screen.getByLabelText("Maintenance date"), {
      target: { value: "2026-07-08" },
    });
    await user.clear(screen.getByLabelText("Odometer"));
    await user.type(screen.getByLabelText("Odometer"), "36000");
    await user.clear(screen.getByLabelText("Total cost"));
    await user.type(screen.getByLabelText("Total cost"), "320.00");
    await user.clear(screen.getByLabelText("Service description"));
    await user.type(screen.getByLabelText("Service description"), "Oil and filter change");
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(updateMaintenanceMock).toHaveBeenCalledWith("vehicle-id", "maintenance-id", {
      maintenanceDate: "2026-07-08",
      odometer: 36000,
      description: "Oil and filter change",
      cost: 320,
    });
    await waitFor(() => expect(assignMock).toHaveBeenCalledWith("/vehicles/vehicle-id"));
  });

  it("shows a not found message when the maintenance does not exist", async () => {
    getMaintenanceMock.mockRejectedValue(new ApiError("Not found", 404));

    render(<MaintenanceEditPage />);

    expect(await screen.findByText("Maintenance record not found.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^back to vehicle$/i })).toHaveAttribute(
      "href",
      "/vehicles/vehicle-id",
    );
  });
});
