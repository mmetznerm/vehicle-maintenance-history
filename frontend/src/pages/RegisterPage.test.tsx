import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiError, register } from "../services/api";
import { RegisterPage } from "./RegisterPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    register: vi.fn(),
  };
});

const registerMock = vi.mocked(register);

describe("RegisterPage", () => {
  beforeEach(() => {
    localStorage.clear();
    registerMock.mockReset();
  });

  it("validates required fields before calling the API", async () => {
    const user = userEvent.setup();

    render(<RegisterPage />);

    await user.click(screen.getByRole("button", { name: /create account/i }));

    expect(registerMock).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent(/enter your full name/i);
  });

  it("validates minimum password length", async () => {
    const user = userEvent.setup();

    render(<RegisterPage />);

    await user.type(screen.getByLabelText("Full name"), "Driver One");
    await user.type(screen.getByLabelText("Email or phone number"), "driver@example.com");
    await user.type(screen.getByLabelText("Password"), "1234567");
    await user.click(screen.getByRole("button", { name: /create account/i }));

    expect(registerMock).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent(/password must be at least 8 characters/i);
  });

  it("translates field errors returned by the API", async () => {
    const user = userEvent.setup();

    registerMock.mockRejectedValue(
      new ApiError("Invalid request", 400, [
        { field: "password", message: "size must be between 8 and 72" },
      ]),
    );

    render(<RegisterPage />);

    await user.type(screen.getByLabelText("Full name"), "Driver One");
    await user.type(screen.getByLabelText("Email or phone number"), "driver@example.com");
    await user.type(screen.getByLabelText("Password"), "secret-password");
    await user.click(screen.getByRole("button", { name: /create account/i }));

    expect(registerMock).toHaveBeenCalledWith({
      fullName: "Driver One",
      emailOrPhone: "driver@example.com",
      password: "secret-password",
    });
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /password must be between 8 and 72 characters/i,
    );
  });
});
