import type { AuthTokensResponse, LoginRequest, LogoutRequest, RegisterRequest } from "../types/auth";
import type {
  CreateMaintenanceRequest,
  Maintenance,
  UpdateMaintenanceRequest,
} from "../types/maintenance";
import type {
  CreateVehicleRequest,
  UpdateVehicleRequest,
  Vehicle,
  VehicleSummary,
} from "../types/vehicle";
import { clearAuthTokens, getAccessToken } from "./authStorage";

const DEFAULT_API_BASE_URL = "";
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
  const isAuthEndpoint = path.startsWith("/v1/auth/");

  headers.set("Content-Type", "application/json");

  if (accessToken && !isAuthEndpoint) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    });
  } catch {
    throw new ApiError(
      "Could not connect to the server. Check whether the API is running.",
      503,
    );
  }

  const body = await parseJsonSafely<ApiErrorResponse | T>(response);

  if (!response.ok) {
    const apiError = body as ApiErrorResponse | null;
    const message = apiError?.message || "Could not complete the request.";

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

export function register(requestBody: RegisterRequest) {
  return request<AuthTokensResponse>("/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function logout(requestBody: LogoutRequest) {
  return request<void>("/v1/auth/logout", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function listVehicles() {
  return request<VehicleSummary[]>("/v1/vehicles");
}

export function getVehicle(vehicleId: string) {
  return request<Vehicle>(`/v1/vehicles/${vehicleId}`);
}

export function createVehicle(requestBody: CreateVehicleRequest) {
  return request<Vehicle>("/v1/vehicles", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function updateVehicle(vehicleId: string, requestBody: UpdateVehicleRequest) {
  return request<Vehicle>(`/v1/vehicles/${vehicleId}`, {
    method: "PUT",
    body: JSON.stringify(requestBody),
  });
}

export function deleteVehicle(vehicleId: string) {
  return request<void>(`/v1/vehicles/${vehicleId}`, {
    method: "DELETE",
  });
}

export function listMaintenances(vehicleId: string) {
  return request<Maintenance[]>(`/v1/vehicles/${vehicleId}/maintenances`);
}

export function getMaintenance(vehicleId: string, maintenanceId: string) {
  return request<Maintenance>(`/v1/vehicles/${vehicleId}/maintenances/${maintenanceId}`);
}

export function createMaintenance(vehicleId: string, requestBody: CreateMaintenanceRequest) {
  return request<Maintenance>(`/v1/vehicles/${vehicleId}/maintenances`, {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function updateMaintenance(
  vehicleId: string,
  maintenanceId: string,
  requestBody: UpdateMaintenanceRequest,
) {
  return request<Maintenance>(`/v1/vehicles/${vehicleId}/maintenances/${maintenanceId}`, {
    method: "PUT",
    body: JSON.stringify(requestBody),
  });
}

export function deleteMaintenance(vehicleId: string, maintenanceId: string) {
  return request<void>(`/v1/vehicles/${vehicleId}/maintenances/${maintenanceId}`, {
    method: "DELETE",
  });
}
