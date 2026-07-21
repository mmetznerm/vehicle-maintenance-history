import { render, screen } from "@testing-library/react";
import { getPublicVehicleHistory } from "../services/api";
import { PublicVehicleHistoryPage } from "./PublicVehicleHistoryPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();
  return { ...actual, getPublicVehicleHistory: vi.fn() };
});

const getPublicVehicleHistoryMock = vi.mocked(getPublicVehicleHistory);

describe("PublicVehicleHistoryPage", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/history/public-id");
  });

  it("renders the sanitized maintenance timeline", async () => {
    getPublicVehicleHistoryMock.mockResolvedValue({
      publicId: "public-id",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2020,
      color: "Preto",
      maintenances: [
        {
          id: "maintenance-id",
          maintenanceDate: "2026-07-21",
          odometer: 45000,
          description: "Troca de óleo",
        },
      ],
    });

    render(<PublicVehicleHistoryPage />);

    expect(await screen.findByRole("heading", { name: /honda civic/i })).toBeInTheDocument();
    expect(screen.getByText("Troca de óleo")).toBeInTheDocument();
    expect(screen.getByText("45.000 km")).toBeInTheDocument();
    expect(screen.getByText(/não representa certificação oficial/i)).toBeInTheDocument();
  });

  it("hides revoked or unknown histories", async () => {
    const { ApiError } = await import("../services/api");
    getPublicVehicleHistoryMock.mockRejectedValue(new ApiError("Not found", 404));

    render(<PublicVehicleHistoryPage />);

    expect(await screen.findByRole("heading", { name: /histórico indisponível/i })).toBeInTheDocument();
    expect(screen.getByText(/não existe ou não está mais compartilhado/i)).toBeInTheDocument();
  });
});
