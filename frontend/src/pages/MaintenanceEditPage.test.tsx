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
      description: "Troca de óleo",
      cost: 250,
    });

    render(<MaintenanceEditPage />);

    expect(screen.getByRole("status")).toHaveTextContent(/carregando manutenção/i);
    expect(await screen.findByDisplayValue("2026-07-07")).toBeInTheDocument();
    expect(screen.getByDisplayValue("35000")).toBeInTheDocument();
    expect(screen.getByDisplayValue("250.00")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Troca de óleo")).toBeInTheDocument();
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
      description: "Troca de óleo",
      cost: 250,
    });
    updateMaintenanceMock.mockResolvedValue({
      id: "maintenance-id",
      vehicleId: "vehicle-id",
      maintenanceDate: "2026-07-08",
      odometer: 36000,
      description: "Troca de óleo e filtro",
      cost: 320,
    });
    vi.stubGlobal("location", {
      ...window.location,
      assign: assignMock,
    });

    render(<MaintenanceEditPage />);

    await screen.findByDisplayValue("Troca de óleo");
    fireEvent.change(screen.getByLabelText("Data da manutenção"), {
      target: { value: "2026-07-08" },
    });
    await user.clear(screen.getByLabelText("Odômetro"));
    await user.type(screen.getByLabelText("Odômetro"), "36000");
    await user.clear(screen.getByLabelText("Custo total"));
    await user.type(screen.getByLabelText("Custo total"), "320,00");
    await user.clear(screen.getByLabelText("Descrição do serviço"));
    await user.type(screen.getByLabelText("Descrição do serviço"), "Troca de óleo e filtro");
    await user.click(screen.getByRole("button", { name: /salvar/i }));

    expect(updateMaintenanceMock).toHaveBeenCalledWith("vehicle-id", "maintenance-id", {
      maintenanceDate: "2026-07-08",
      odometer: 36000,
      description: "Troca de óleo e filtro",
      cost: 320,
    });
    await waitFor(() => expect(assignMock).toHaveBeenCalledWith("/vehicles/vehicle-id"));
  });

  it("shows a not found message when the maintenance does not exist", async () => {
    getMaintenanceMock.mockRejectedValue(new ApiError("Not found", 404));

    render(<MaintenanceEditPage />);

    expect(await screen.findByText("Manutenção não encontrada.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /voltar para veículo/i })).toHaveAttribute(
      "href",
      "/vehicles/vehicle-id",
    );
  });
});
