import { FormEvent, useState } from "react";
import {
  ArrowRightIcon,
  EyeIcon,
  EyeOffIcon,
  LockIcon,
  UserIcon,
} from "../components/Icons";
import { TextField } from "../components/TextField";
import { VehicleHistoryMark } from "../components/VehicleHistoryLogo";
import { ApiError, login } from "../services/api";
import { saveAuthTokens } from "../services/authStorage";

function getLoginErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401 || error.status === 403) {
      return "Email, phone number, or password is invalid.";
    }

    if (error.status >= 500) {
      return "Could not connect to the server. Check whether the API is running.";
    }

    if (error.fieldErrors.length > 0) {
      return error.fieldErrors.map((fieldError) => fieldError.message).join(" ");
    }

    return error.message;
  }

  return "Could not connect to the server. Check whether the API is running.";
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
      setErrorMessage("Enter your email or phone number and password.");
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
          <div className="brand-lockup">
            <VehicleHistoryMark />
            <h1 id="login-title">Vehicle History</h1>
          </div>
          <p>Efficient fleet management</p>
        </header>

        <form className="login-form" onSubmit={handleSubmit} noValidate>
          <TextField
            id="emailOrPhone"
            label="Email or phone number"
            value={emailOrPhone}
            placeholder="name@company.com"
            autoComplete="username"
            required
            leadingIcon={<UserIcon aria-hidden />}
            onChange={setEmailOrPhone}
          />

          <div className="password-row">
            <TextField
              id="password"
              label="Password"
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
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  onClick={() => setShowPassword((current) => !current)}
                >
                  {showPassword ? <EyeOffIcon aria-hidden /> : <EyeIcon aria-hidden />}
                </button>
              }
              onChange={setPassword}
            />
            <a className="forgot-link" href="#forgot-password" onClick={(event) => event.preventDefault()}>
              Forgot your password?
            </a>
          </div>

          {errorMessage ? (
            <p className="form-error" role="alert">
              {errorMessage}
            </p>
          ) : null}

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            <span>{isSubmitting ? "Signing in..." : "Sign in"}</span>
            <ArrowRightIcon aria-hidden />
          </button>
        </form>

        <footer className="auth-card-footer">
          <span>Don't have an account?</span>
          <a href="/register">Sign up here</a>
        </footer>
      </section>
    </main>
  );
}
