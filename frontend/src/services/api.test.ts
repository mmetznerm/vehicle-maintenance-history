import {
  ApiError,
  createMaintenance,
  createVehicle,
  deleteVehicle,
  deleteMaintenance,
  getVehicle,
  listMaintenances,
  listVehicles,
  login,
  register,
  updateVehicle,
} from "./api";
import { saveAuthTokens } from "./authStorage";

describe("api service", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it("posts login credentials without an authorization header", async () => {
    saveAuthTokens({
      accessToken: "existing-access-token",
      refreshToken: "existing-refresh-token",
    });

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          accessToken: "new-access-token",
          refreshToken: "new-refresh-token",
        }),
        { status: 200 },
      ),
    );

    vi.stubGlobal("fetch", fetchMock);

    await expect(
      login({
        emailOrPhone: "driver@example.com",
        password: "secret-password",
      }),
    ).resolves.toEqual({
      accessToken: "new-access-token",
      refreshToken: "new-refresh-token",
    });

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;

    expect(fetchMock).toHaveBeenCalledWith("/v1/auth/login", expect.any(Object));
    expect(headers.get("Content-Type")).toBe("application/json");
    expect(headers.get("Authorization")).toBeNull();
  });

  it("maps api error responses to ApiError", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            message: "Invalid request",
            fieldErrors: [{ field: "password", message: "must not be blank" }],
          }),
          { status: 400 },
        ),
      ),
    );

    await expect(
      register({
        fullName: "",
        emailOrPhone: "",
        password: "",
      }),
    ).rejects.toMatchObject({
      name: "ApiError",
      message: "Invalid request",
      status: 400,
      fieldErrors: [{ field: "password", message: "must not be blank" }],
    });
  });

  it("uses a service unavailable error when fetch fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("network down")));

    await expect(
      login({
        emailOrPhone: "driver@example.com",
        password: "secret-password",
      }),
    ).rejects.toBeInstanceOf(ApiError);
  });

  it("lists vehicles with the authorization header", async () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    const vehicles = [
      {
        id: "vehicle-id",
        plate: "ABC1234",
        brand: "Honda",
        model: "Civic",
        manufactureYear: 2020,
        color: "Prata",
      },
    ];
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(vehicles), { status: 200 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(listVehicles()).resolves.toEqual(vehicles);

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;

    expect(fetchMock).toHaveBeenCalledWith("/v1/vehicles", expect.any(Object));
    expect(headers.get("Authorization")).toBe("Bearer access-token");
  });

  it("deletes a vehicle", async () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(deleteVehicle("vehicle-id")).resolves.toBeNull();

    const [, options] = fetchMock.mock.calls[0];

    expect(fetchMock).toHaveBeenCalledWith("/v1/vehicles/vehicle-id", expect.any(Object));
    expect(options.method).toBe("DELETE");
  });

  it("creates a vehicle with the authorization header", async () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    const vehicle = {
      id: "vehicle-id",
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2020,
      color: "Prata",
    };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(vehicle), { status: 201 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(
      createVehicle({
        plate: "ABC1234",
        brand: "Honda",
        model: "Civic",
        manufactureYear: 2020,
        color: "Prata",
      }),
    ).resolves.toEqual(vehicle);

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;

    expect(fetchMock).toHaveBeenCalledWith("/v1/vehicles", expect.any(Object));
    expect(options.method).toBe("POST");
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(JSON.parse(options.body as string)).toEqual({
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2020,
      color: "Prata",
    });
  });

  it("gets a vehicle by id", async () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    const vehicle = {
      id: "vehicle-id",
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2020,
      color: "Prata",
    };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(vehicle), { status: 200 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(getVehicle("vehicle-id")).resolves.toEqual(vehicle);

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;

    expect(fetchMock).toHaveBeenCalledWith("/v1/vehicles/vehicle-id", expect.any(Object));
    expect(headers.get("Authorization")).toBe("Bearer access-token");
  });

  it("updates a vehicle", async () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    const vehicle = {
      id: "vehicle-id",
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2021,
      color: "Preto",
    };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(vehicle), { status: 200 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateVehicle("vehicle-id", {
        plate: "ABC1234",
        brand: "Honda",
        model: "Civic",
        manufactureYear: 2021,
        color: "Preto",
      }),
    ).resolves.toEqual(vehicle);

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;

    expect(fetchMock).toHaveBeenCalledWith("/v1/vehicles/vehicle-id", expect.any(Object));
    expect(options.method).toBe("PUT");
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(JSON.parse(options.body as string)).toEqual({
      plate: "ABC1234",
      brand: "Honda",
      model: "Civic",
      manufactureYear: 2021,
      color: "Preto",
    });
  });

  it("lists maintenances for a vehicle", async () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    const maintenances = [
      {
        id: "maintenance-id",
        vehicleId: "vehicle-id",
        maintenanceDate: "2026-07-07",
        odometer: 35000,
        description: "Troca de óleo",
        cost: 250,
      },
    ];
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(maintenances), { status: 200 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(listMaintenances("vehicle-id")).resolves.toEqual(maintenances);

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;

    expect(fetchMock).toHaveBeenCalledWith(
      "/v1/vehicles/vehicle-id/maintenances",
      expect.any(Object),
    );
    expect(headers.get("Authorization")).toBe("Bearer access-token");
  });

  it("creates a maintenance with the authorization header", async () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    const maintenance = {
      id: "maintenance-id",
      vehicleId: "vehicle-id",
      maintenanceDate: "2026-07-07",
      odometer: 35000,
      description: "Troca de óleo",
      cost: 250,
    };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(maintenance), { status: 201 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(
      createMaintenance("vehicle-id", {
        maintenanceDate: "2026-07-07",
        odometer: 35000,
        description: "Troca de óleo",
        cost: 250,
      }),
    ).resolves.toEqual(maintenance);

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;

    expect(fetchMock).toHaveBeenCalledWith(
      "/v1/vehicles/vehicle-id/maintenances",
      expect.any(Object),
    );
    expect(options.method).toBe("POST");
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(JSON.parse(options.body as string)).toEqual({
      maintenanceDate: "2026-07-07",
      odometer: 35000,
      description: "Troca de óleo",
      cost: 250,
    });
  });

  it("deletes a maintenance", async () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(deleteMaintenance("vehicle-id", "maintenance-id")).resolves.toBeNull();

    const [, options] = fetchMock.mock.calls[0];

    expect(fetchMock).toHaveBeenCalledWith(
      "/v1/vehicles/vehicle-id/maintenances/maintenance-id",
      expect.any(Object),
    );
    expect(options.method).toBe("DELETE");
  });
});
