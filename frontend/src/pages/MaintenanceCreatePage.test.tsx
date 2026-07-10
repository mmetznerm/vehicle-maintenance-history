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

    await user.click(screen.getByRole("button", { name: /salvar/i }));

    expect(createMaintenanceMock).not.toHaveBeenCalled();
    expect(screen.getByText(/revise os campos destacados/i)).toBeInTheDocument();
    expect(screen.getByText(/informe o od.metro/i)).toBeInTheDocument();
    expect(screen.getByText(/informe o custo total/i)).toBeInTheDocument();
    expect(screen.getByText(/informe a descri..o do servi.o/i)).toBeInTheDocument();
  });

  it("submits the maintenance payload and redirects to vehicle details", async () => {
    const user = userEvent.setup();
    const assignMock = vi.fn();

    createMaintenanceMock.mockResolvedValue({
      id: "maintenance-id",
      vehicleId: "vehicle-id",
      maintenanceDate: "2026-07-07",
      odometer: 35000,
      description: "Troca de óleo",
      cost: 250,
    });
    vi.stubGlobal("location", {
      ...window.location,
      assign: assignMock,
    });

    render(<MaintenanceCreatePage />);

    fireEvent.change(screen.getByLabelText("Data da manutenção"), {
      target: { value: "2026-07-07" },
    });
    await user.type(screen.getByLabelText("Odômetro"), "35000");
    await user.type(screen.getByLabelText("Custo total"), "250,00");
    await user.type(screen.getByLabelText("Descrição do serviço"), "Troca de óleo");
    await user.click(screen.getByRole("button", { name: /salvar/i }));

    expect(createMaintenanceMock).toHaveBeenCalledWith("vehicle-id", {
      maintenanceDate: "2026-07-07",
      odometer: 35000,
      description: "Troca de óleo",
      cost: 250,
    });
    await waitFor(() => expect(assignMock).toHaveBeenCalledWith("/vehicles/vehicle-id"));
  });

  it("shows field errors returned by the API", async () => {
    const user = userEvent.setup();

    createMaintenanceMock.mockRejectedValue(
      new ApiError("Invalid request", 400, [{ field: "cost", message: "Custo inválido." }]),
    );

    render(<MaintenanceCreatePage />);

    fireEvent.change(screen.getByLabelText("Data da manutenção"), {
      target: { value: "2026-07-07" },
    });
    await user.type(screen.getByLabelText("Odômetro"), "35000");
    await user.type(screen.getByLabelText("Custo total"), "250,00");
    await user.type(screen.getByLabelText("Descrição do serviço"), "Troca de óleo");
    await user.click(screen.getByRole("button", { name: /salvar/i }));

    expect(await screen.findByText("Custo inválido.")).toBeInTheDocument();
  });
});
