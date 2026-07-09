import { FormEvent, useState } from "react";
import { CarIcon, LockIcon, MailIcon, UserIcon } from "../components/Icons";
import { TextField } from "../components/TextField";
import { ApiError, register } from "../services/api";
import { saveAuthTokens } from "../services/authStorage";

const FIELD_LABELS: Record<string, string> = {
  fullName: "nome completo",
  emailOrPhone: "e-mail ou telefone",
  password: "senha",
};

function translateFieldError(field: string, message: string) {
  const label = FIELD_LABELS[field] ?? field;
  const normalizedMessage = message.toLowerCase();

  if (normalizedMessage.includes("must not be blank")) {
    return `Informe ${label}.`;
  }

  if (field === "password" && normalizedMessage.includes("size must be between")) {
    return "A senha deve ter entre 8 e 72 caracteres.";
  }

  if (field === "fullName" && normalizedMessage.includes("size must be between")) {
    return "O nome completo deve ter no máximo 160 caracteres.";
  }

  if (field === "emailOrPhone" && normalizedMessage.includes("size must be between")) {
    return "O e-mail ou telefone deve ter no máximo 180 caracteres.";
  }

  return `${label}: ${message}`;
}

function getRegisterErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 409) {
      return "Usuário já cadastrado. Volte ao login ou use outro e-mail ou telefone.";
    }

    if (error.status >= 500) {
      return "Não foi possível conectar ao servidor. Verifique se a API está em execução.";
    }

    if (error.fieldErrors.length > 0) {
      return error.fieldErrors
        .map((fieldError) => translateFieldError(fieldError.field, fieldError.message))
        .join(" ");
    }

    if (error.message === "Não foi possível concluir a solicitação.") {
      return "Não foi possível criar a conta agora. Verifique se a API está em execução.";
    }

    return error.message;
  }

  return "Não foi possível conectar ao servidor. Verifique se a API está em execução.";
}

export function RegisterPage() {
  const [fullName, setFullName] = useState("");
  const [emailOrPhone, setEmailOrPhone] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage("");

    if (!fullName.trim() || !emailOrPhone.trim() || !password) {
      setErrorMessage("Informe nome completo, e-mail ou telefone e senha.");
      return;
    }

    if (password.length < 8) {
      setErrorMessage("A senha deve ter pelo menos 8 caracteres.");
      return;
    }

    setIsSubmitting(true);

    try {
      const tokens = await register({
        fullName: fullName.trim(),
        emailOrPhone: emailOrPhone.trim(),
        password,
      });

      saveAuthTokens(tokens);
      window.location.assign("/vehicles");
    } catch (error) {
      setErrorMessage(getRegisterErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-page" aria-labelledby="register-title">
      <section className="auth-card register-card">
        <header className="auth-card-header register-card-header">
          <span className="brand-icon-badge">
            <CarIcon className="brand-icon" aria-hidden />
          </span>
          <h1 id="register-title">AutoLog</h1>
          <p>Crie sua conta para gerenciar sua frota.</p>
        </header>

        <form className="login-form register-form" onSubmit={handleSubmit} noValidate>
          <TextField
            id="fullName"
            label="Nome completo"
            value={fullName}
            placeholder="João Silva"
            autoComplete="name"
            required
            leadingIcon={<UserIcon aria-hidden />}
            onChange={setFullName}
          />

          <TextField
            id="registerEmailOrPhone"
            label="E-mail ou telefone"
            value={emailOrPhone}
            placeholder="joao@empresa.com"
            autoComplete="username"
            required
            leadingIcon={<MailIcon aria-hidden />}
            onChange={setEmailOrPhone}
          />

          <div>
            <TextField
              id="registerPassword"
              label="Senha"
              type="password"
              value={password}
              placeholder="••••••••"
              autoComplete="new-password"
              required
              leadingIcon={<LockIcon aria-hidden />}
              onChange={setPassword}
            />
            <p className="field-help">Deve ter pelo menos 8 caracteres.</p>
          </div>

          {errorMessage ? (
            <p className="form-error" role="alert">
              {errorMessage}
            </p>
          ) : null}

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            <span>{isSubmitting ? "Criando conta..." : "Criar conta"}</span>
          </button>
        </form>

        <footer className="auth-card-footer register-card-footer">
          <span>Já tem uma conta?</span>
          <a href="/login">Entrar</a>
        </footer>
      </section>
    </main>
  );
}
