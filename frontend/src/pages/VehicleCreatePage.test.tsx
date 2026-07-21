import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiError, createVehicle } from "../services/api";
import { VehicleCreatePage } from "./VehicleCreatePage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    createVehicle: vi.fn(),
  };
});

const createVehicleMock = vi.mocked(createVehicle);

describe("VehicleCreatePage", () => {
  beforeEach(() => {
    localStorage.clear();
    createVehicleMock.mockReset();
  });

  it("validates required fields before calling the API", async () => {
    const user = userEvent.setup();

    render(<VehicleCreatePage />);

    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(createVehicleMock).not.toHaveBeenCalled();
    expect(screen.getByText(/review the highlighted fields/i)).toBeInTheDocument();
    expect(screen.getByText(/enter the license plate/i)).toBeInTheDocument();
  });

  it("submits the normalized vehicle payload", async () => {
    const user = userEvent.setup();

    createVehicleMock.mockReturnValue(new Promise(() => undefined));

    render(<VehicleCreatePage />);

    await user.type(screen.getByLabelText("License plate"), "abc1234");
    await user.type(screen.getByLabelText("Brand"), "Honda");
    await user.type(screen.getByLabelText("Model"), "Civic");
    await user.type(screen.getByLabelText("Year"), "2020");
    await user.type(screen.getByLabelText("Color"), "Silver");
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(createVehicleMock).toHaveBeenCalledWith({
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2020,
      color: "Silver",
    });
  });

  it("shows field errors returned by the API", async () => {
    const user = userEvent.setup();

    createVehicleMock.mockRejectedValue(
      new ApiError("Invalid request", 400, [{ field: "plate", message: "License plate already registered." }]),
    );

    render(<VehicleCreatePage />);

    await user.type(screen.getByLabelText("License plate"), "ABC1234");
    await user.type(screen.getByLabelText("Brand"), "Honda");
    await user.type(screen.getByLabelText("Model"), "Civic");
    await user.type(screen.getByLabelText("Year"), "2020");
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText("License plate already registered.")).toBeInTheDocument();
  });
});
