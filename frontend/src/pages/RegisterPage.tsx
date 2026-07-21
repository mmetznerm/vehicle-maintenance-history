import { FormEvent, useState } from "react";
import { CarIcon, LockIcon, MailIcon, UserIcon } from "../components/Icons";
import { TextField } from "../components/TextField";
import { ApiError, register } from "../services/api";
import { saveAuthTokens } from "../services/authStorage";

const FIELD_LABELS: Record<string, string> = {
  fullName: "your full name",
  emailOrPhone: "your email or phone number",
  password: "your password",
};

function translateFieldError(field: string, message: string) {
  const label = FIELD_LABELS[field] ?? field;
  const normalizedMessage = message.toLowerCase();

  if (normalizedMessage.includes("must not be blank")) {
    return `Enter ${label}.`;
  }

  if (field === "password" && normalizedMessage.includes("size must be between")) {
    return "Password must be between 8 and 72 characters.";
  }

  if (field === "fullName" && normalizedMessage.includes("size must be between")) {
    return "Full name must be no more than 160 characters.";
  }

  if (field === "emailOrPhone" && normalizedMessage.includes("size must be between")) {
    return "Email or phone number must be no more than 180 characters.";
  }

  return `${label}: ${message}`;
}

function getRegisterErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 409) {
      return "User is already registered. Return to sign in or use another email or phone number.";
    }

    if (error.status >= 500) {
      return "Could not connect to the server. Check whether the API is running.";
    }

    if (error.fieldErrors.length > 0) {
      return error.fieldErrors
        .map((fieldError) => translateFieldError(fieldError.field, fieldError.message))
        .join(" ");
    }

    if (error.message === "Could not complete the request.") {
      return "Could not create the account. Check whether the API is running.";
    }

    return error.message;
  }

  return "Could not connect to the server. Check whether the API is running.";
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
      setErrorMessage("Enter your full name, email or phone number, and password.");
      return;
    }

    if (password.length < 8) {
      setErrorMessage("Password must be at least 8 characters long.");
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
          <p>Create your account to manage your fleet.</p>
        </header>

        <form className="login-form register-form" onSubmit={handleSubmit} noValidate>
          <TextField
            id="fullName"
            label="Full name"
            value={fullName}
            placeholder="John Smith"
            autoComplete="name"
            required
            leadingIcon={<UserIcon aria-hidden />}
            onChange={setFullName}
          />

          <TextField
            id="registerEmailOrPhone"
            label="Email or phone number"
            value={emailOrPhone}
            placeholder="john@company.com"
            autoComplete="username"
            required
            leadingIcon={<MailIcon aria-hidden />}
            onChange={setEmailOrPhone}
          />

          <div>
            <TextField
              id="registerPassword"
              label="Password"
              type="password"
              value={password}
              placeholder="••••••••"
              autoComplete="new-password"
              required
              leadingIcon={<LockIcon aria-hidden />}
              onChange={setPassword}
            />
            <p className="field-help">Must be at least 8 characters long.</p>
          </div>

          {errorMessage ? (
            <p className="form-error" role="alert">
              {errorMessage}
            </p>
          ) : null}

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            <span>{isSubmitting ? "Creating account..." : "Create account"}</span>
          </button>
        </form>

        <footer className="auth-card-footer register-card-footer">
          <span>Already have an account?</span>
          <a href="/login">Sign in</a>
        </footer>
      </section>
    </main>
  );
}
