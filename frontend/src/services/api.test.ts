import { ApiError, deleteVehicle, listVehicles, login, register } from "./api";
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
});
