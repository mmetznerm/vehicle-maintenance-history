import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiError, getVehicle, updateVehicle } from "../services/api";
import { VehicleEditPage } from "./VehicleEditPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    getVehicle: vi.fn(),
    updateVehicle: vi.fn(),
  };
});

const getVehicleMock = vi.mocked(getVehicle);
const updateVehicleMock = vi.mocked(updateVehicle);

function setPath(path: string) {
  window.history.pushState({}, "", path);
}

describe("VehicleEditPage", () => {
  beforeEach(() => {
    localStorage.clear();
    getVehicleMock.mockReset();
    updateVehicleMock.mockReset();
    setPath("/vehicles/vehicle-id/edit");
  });

  it("loads the vehicle and fills the form", async () => {
    getVehicleMock.mockResolvedValue({
      id: "vehicle-id",
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2020,
      color: "Silver",
    });

    render(<VehicleEditPage />);

    expect(screen.getByRole("status")).toHaveTextContent(/loading vehicle/i);
    expect(await screen.findByDisplayValue("ABC1234")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Honda")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Civic")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2020")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Silver")).toBeInTheDocument();
    expect(getVehicleMock).toHaveBeenCalledWith("vehicle-id");
  });

  it("submits updated vehicle data", async () => {
    const user = userEvent.setup();

    getVehicleMock.mockResolvedValue({
      id: "vehicle-id",
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2020,
      color: "Silver",
    });
    updateVehicleMock.mockReturnValue(new Promise(() => undefined));

    render(<VehicleEditPage />);

    await screen.findByDisplayValue("ABC1234");
    await user.clear(screen.getByLabelText("Color"));
    await user.type(screen.getByLabelText("Color"), "Black");
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(updateVehicleMock).toHaveBeenCalledWith("vehicle-id", {
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2020,
      color: "Black",
    });
  });

  it("shows a not found message when the vehicle does not exist", async () => {
    getVehicleMock.mockRejectedValue(new ApiError("Not found", 404));

    render(<VehicleEditPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent(/vehicle not found/i);
    const backLinks = screen.getAllByRole("link", { name: /back to vehicles/i });

    expect(backLinks).toHaveLength(2);
    backLinks.forEach((link) => {
      expect(link).toHaveAttribute("href", "/vehicles");
    });
  });
});
