import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiError, login } from "../services/api";
import { LoginPage } from "./LoginPage";

vi.mock("../services/api", async (importActual) => {
  const actual = await importActual<typeof import("../services/api")>();

  return {
    ...actual,
    login: vi.fn(),
  };
});

const loginMock = vi.mocked(login);

describe("LoginPage", () => {
  beforeEach(() => {
    localStorage.clear();
    loginMock.mockReset();
  });

  it("validates required credentials before calling the API", async () => {
    const user = userEvent.setup();

    render(<LoginPage />);

    await user.click(screen.getByRole("button", { name: /sign in/i }));

    expect(loginMock).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent(/enter your email or phone number and password/i);
  });

  it("toggles password visibility", async () => {
    const user = userEvent.setup();

    render(<LoginPage />);

    const passwordInput = screen.getByLabelText("Password");

    expect(passwordInput).toHaveAttribute("type", "password");

    await user.click(screen.getByRole("button", { name: /show password/i }));

    expect(passwordInput).toHaveAttribute("type", "text");

    await user.click(screen.getByRole("button", { name: /hide password/i }));

    expect(passwordInput).toHaveAttribute("type", "password");
  });

  it("shows field errors returned by the API", async () => {
    const user = userEvent.setup();

    loginMock.mockRejectedValue(
      new ApiError("Invalid request", 400, [{ field: "password", message: "Password is required." }]),
    );

    render(<LoginPage />);

    await user.type(screen.getByLabelText("Email or phone number"), "driver@example.com");
    await user.type(screen.getByLabelText("Password"), "secret-password");
    await user.click(screen.getByRole("button", { name: /sign in/i }));

    expect(loginMock).toHaveBeenCalledWith({
      emailOrPhone: "driver@example.com",
      password: "secret-password",
    });
    expect(await screen.findByRole("alert")).toHaveTextContent("Password is required.");
  });
});
