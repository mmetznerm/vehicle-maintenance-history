import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiError, createMaintenance } from "../services/api";
import { MaintenanceCreatePage } from "./MaintenanceCreatePage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    createMaintenance: vi.fn(),
  };
});

const createMaintenanceMock = vi.mocked(createMaintenance);

function setPath(path: string) {
  window.history.pushState({}, "", path);
}

describe("MaintenanceCreatePage", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    createMaintenanceMock.mockReset();
    setPath("/vehicles/vehicle-id/maintenances/new");
  });

  it("validates required fields before calling the API", async () => {
    const user = userEvent.setup();

    render(<MaintenanceCreatePage />);

    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(createMaintenanceMock).not.toHaveBeenCalled();
    expect(screen.getByText(/review the highlighted fields/i)).toBeInTheDocument();
    expect(screen.getByText(/enter the odometer reading/i)).toBeInTheDocument();
    expect(screen.getByText(/enter the total cost/i)).toBeInTheDocument();
    expect(screen.getByText(/enter the service description/i)).toBeInTheDocument();
  });

  it("submits the maintenance payload and redirects to vehicle details", async () => {
    const user = userEvent.setup();
    const assignMock = vi.fn();

    createMaintenanceMock.mockResolvedValue({
      id: "maintenance-id",
      vehicleId: "vehicle-id",
      maintenanceDate: "2026-07-07",
      odometer: 35000,
      description: "Oil change",
      cost: 250,
    });
    vi.stubGlobal("location", {
      ...window.location,
      assign: assignMock,
    });

    render(<MaintenanceCreatePage />);

    fireEvent.change(screen.getByLabelText("Maintenance date"), {
      target: { value: "2026-07-07" },
    });
    await user.type(screen.getByLabelText("Odometer"), "35000");
    await user.type(screen.getByLabelText("Total cost"), "250.00");
    await user.type(screen.getByLabelText("Service description"), "Oil change");
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(createMaintenanceMock).toHaveBeenCalledWith("vehicle-id", {
      maintenanceDate: "2026-07-07",
      odometer: 35000,
      description: "Oil change",
      cost: 250,
    });
    await waitFor(() => expect(assignMock).toHaveBeenCalledWith("/vehicles/vehicle-id"));
  });

  it("shows field errors returned by the API", async () => {
    const user = userEvent.setup();

    createMaintenanceMock.mockRejectedValue(
      new ApiError("Invalid request", 400, [{ field: "cost", message: "Invalid cost." }]),
    );

    render(<MaintenanceCreatePage />);

    fireEvent.change(screen.getByLabelText("Maintenance date"), {
      target: { value: "2026-07-07" },
    });
    await user.type(screen.getByLabelText("Odometer"), "35000");
    await user.type(screen.getByLabelText("Total cost"), "250.00");
    await user.type(screen.getByLabelText("Service description"), "Oil change");
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText("Invalid cost.")).toBeInTheDocument();
  });
});
