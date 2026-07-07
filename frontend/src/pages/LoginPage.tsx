import { FormEvent, useState } from "react";
import { ArrowRightIcon, CarIcon, EyeIcon, EyeOffIcon, LockIcon, UserIcon } from "../components/Icons";
import { TextField } from "../components/TextField";
import { ApiError, login } from "../services/api";
import { saveAuthTokens } from "../services/authStorage";

function getLoginErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return "E-mail, telefone ou senha inválidos.";
    }

    if (error.status >= 500) {
      return "Não foi possível conectar ao servidor. Verifique se a API está em execução.";
    }

    if (error.fieldErrors.length > 0) {
      return error.fieldErrors.map((fieldError) => fieldError.message).join(" ");
    }

    return error.message;
  }

  return "Não foi possível conectar ao servidor. Verifique se a API está em execução.";
}

export function LoginPage() {
  const [emailOrPhone, setEmailOrPhone] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage("");

    if (!emailOrPhone.trim() || !password) {
      setErrorMessage("Informe e-mail ou telefone e senha.");
      return;
    }

    setIsSubmitting(true);

    try {
      const tokens = await login({
        emailOrPhone: emailOrPhone.trim(),
        password,
      });

      saveAuthTokens(tokens);
      window.location.assign("/vehicles");
    } catch (error) {
      setErrorMessage(getLoginErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-page" aria-labelledby="login-title">
      <section className="auth-card">
        <header className="auth-card-header">
          <CarIcon className="brand-icon" aria-hidden />
          <h1 id="login-title">AutoLog</h1>
          <p>Gestão eficiente da sua frota</p>
        </header>

        <form className="login-form" onSubmit={handleSubmit} noValidate>
          <TextField
            id="emailOrPhone"
            label="E-mail ou telefone"
            value={emailOrPhone}
            placeholder="nome@empresa.com"
            autoComplete="username"
            required
            leadingIcon={<UserIcon aria-hidden />}
            onChange={setEmailOrPhone}
          />

          <div className="password-row">
            <TextField
              id="password"
              label="Senha"
              type={showPassword ? "text" : "password"}
              value={password}
              placeholder="••••••••"
              autoComplete="current-password"
              required
              leadingIcon={<LockIcon aria-hidden />}
              trailingAction={
                <button
                  className="icon-button"
                  type="button"
                  aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                  onClick={() => setShowPassword((current) => !current)}
                >
                  {showPassword ? <EyeOffIcon aria-hidden /> : <EyeIcon aria-hidden />}
                </button>
              }
              onChange={setPassword}
            />
            <a className="forgot-link" href="#forgot-password" onClick={(event) => event.preventDefault()}>
              Esqueceu sua senha?
            </a>
          </div>

          {errorMessage ? (
            <p className="form-error" role="alert">
              {errorMessage}
            </p>
          ) : null}

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            <span>{isSubmitting ? "Entrando..." : "Entrar"}</span>
            <ArrowRightIcon aria-hidden />
          </button>
        </form>

        <footer className="auth-card-footer">
          <span>Não tem uma conta?</span>
          <a href="/register">
            Cadastre-se aqui
          </a>
        </footer>
      </section>
    </main>
  );
}
