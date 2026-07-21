import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  deleteMaintenance,
  deleteVehicle,
  disableVehicleHistorySharing,
  enableVehicleHistorySharing,
  getVehicleHistorySharing,
  getVehicle,
  listMaintenanceInconsistencies,
  listMaintenances,
} from "../services/api";
import { VehicleDetailsPage } from "./VehicleDetailsPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    deleteMaintenance: vi.fn(),
    deleteVehicle: vi.fn(),
    disableVehicleHistorySharing: vi.fn(),
    enableVehicleHistorySharing: vi.fn(),
    getVehicleHistorySharing: vi.fn(),
    getVehicle: vi.fn(),
    listMaintenanceInconsistencies: vi.fn(),
    listMaintenances: vi.fn(),
  };
});

const deleteMaintenanceMock = vi.mocked(deleteMaintenance);
const deleteVehicleMock = vi.mocked(deleteVehicle);
const disableVehicleHistorySharingMock = vi.mocked(disableVehicleHistorySharing);
const enableVehicleHistorySharingMock = vi.mocked(enableVehicleHistorySharing);
const getVehicleHistorySharingMock = vi.mocked(getVehicleHistorySharing);
const getVehicleMock = vi.mocked(getVehicle);
const listMaintenanceInconsistenciesMock = vi.mocked(listMaintenanceInconsistencies);
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
    getVehicleHistorySharingMock.mockResolvedValue({ enabled: false, publicId: null });
    listMaintenanceInconsistenciesMock.mockResolvedValue([]);
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
    expect(screen.getByRole("link", { name: "AutoLog" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sair/i })).toBeInTheDocument();
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

  it("enables and disables public history sharing", async () => {
    const user = userEvent.setup();
    listMaintenancesMock.mockResolvedValue([]);
    enableVehicleHistorySharingMock.mockResolvedValue({
      enabled: true,
      publicId: "public-history-id",
    });
    disableVehicleHistorySharingMock.mockResolvedValue(undefined);

    render(<VehicleDetailsPage />);

    await user.click(await screen.findByRole("button", { name: /ativar compartilhamento/i }));

    expect(enableVehicleHistorySharingMock).toHaveBeenCalledWith("vehicle-id");
    expect(await screen.findByRole("link", { name: /visualizar histórico/i })).toHaveAttribute(
      "href",
      "/history/public-history-id",
    );

    await user.click(screen.getByRole("button", { name: /desativar/i }));

    expect(disableVehicleHistorySharingMock).toHaveBeenCalledWith("vehicle-id");
    expect(await screen.findByRole("button", { name: /ativar compartilhamento/i })).toBeInTheDocument();
  });

  it("shows active inconsistency alerts and loads resolved alerts on demand", async () => {
    const user = userEvent.setup();
    listMaintenancesMock.mockResolvedValue([]);
    listMaintenanceInconsistenciesMock
      .mockResolvedValueOnce([
        {
          alertId: "alert-id",
          rule: "ODOMETER_ROLLBACK",
          severity: "CRITICAL",
          maintenanceIds: ["maintenance-id"],
          summary: "Odometer reading decreased",
          details: "Reading changed from 50000 km to 40000 km.",
          status: "ACTIVE",
          detectedAt: "2026-07-20T12:00:00Z",
          resolvedAt: null,
        },
      ])
      .mockResolvedValueOnce([]);

    render(<VehicleDetailsPage />);

    expect(await screen.findByText("Quilometragem regressiva")).toBeInTheDocument();
    expect(screen.getByText("1 alerta ativo")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /mostrar resolvidos/i }));

    expect(listMaintenanceInconsistenciesMock).toHaveBeenLastCalledWith("vehicle-id", true);
    expect(await screen.findByText(/nenhuma inconsistência ativa/i)).toBeInTheDocument();
  });
});
