import type { AuthTokensResponse, LoginRequest } from "../types/auth";
import { clearAuthTokens, getAccessToken } from "./authStorage";

const DEFAULT_API_BASE_URL = "/api";
const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL;
const API_BASE_URL = configuredBaseUrl.replace(/\/$/, "");

type FieldErrorResponse = {
  field: string;
  message: string;
};

type ApiErrorResponse = {
  status?: number;
  message?: string;
  fieldErrors?: FieldErrorResponse[];
};

export class ApiError extends Error {
  status: number;
  fieldErrors: FieldErrorResponse[];

  constructor(message: string, status: number, fieldErrors: FieldErrorResponse[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function parseJsonSafely<T>(response: Response): Promise<T | null> {
  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as T;
  } catch {
    return null;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const accessToken = getAccessToken();
  const headers = new Headers(options.headers);

  headers.set("Content-Type", "application/json");

  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  const body = await parseJsonSafely<ApiErrorResponse | T>(response);

  if (!response.ok) {
    const apiError = body as ApiErrorResponse | null;
    const message = apiError?.message || "Não foi possível concluir a solicitação.";

    if (response.status === 401 && path !== "/v1/auth/login") {
      clearAuthTokens();
      window.location.replace("/login");
    }

    throw new ApiError(message, response.status, apiError?.fieldErrors ?? []);
  }

  return body as T;
}

export function login(requestBody: LoginRequest) {
  return request<AuthTokensResponse>("/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}
