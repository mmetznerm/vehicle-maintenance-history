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

    await user.click(screen.getByRole("button", { name: /entrar/i }));

    expect(loginMock).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent(/informe e-mail ou telefone e senha/i);
  });

  it("toggles password visibility", async () => {
    const user = userEvent.setup();

    render(<LoginPage />);

    const passwordInput = screen.getByLabelText("Senha");

    expect(passwordInput).toHaveAttribute("type", "password");

    await user.click(screen.getByRole("button", { name: /mostrar senha/i }));

    expect(passwordInput).toHaveAttribute("type", "text");

    await user.click(screen.getByRole("button", { name: /ocultar senha/i }));

    expect(passwordInput).toHaveAttribute("type", "password");
  });

  it("shows field errors returned by the API", async () => {
    const user = userEvent.setup();

    loginMock.mockRejectedValue(
      new ApiError("Invalid request", 400, [{ field: "password", message: "Senha obrigatoria." }]),
    );

    render(<LoginPage />);

    await user.type(screen.getByLabelText("E-mail ou telefone"), "driver@example.com");
    await user.type(screen.getByLabelText("Senha"), "secret-password");
    await user.click(screen.getByRole("button", { name: /entrar/i }));

    expect(loginMock).toHaveBeenCalledWith({
      emailOrPhone: "driver@example.com",
      password: "secret-password",
    });
    expect(await screen.findByRole("alert")).toHaveTextContent("Senha obrigatoria.");
  });
});
