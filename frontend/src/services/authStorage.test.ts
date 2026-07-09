import {
  clearAuthTokens,
  clearTokens,
  getAccessToken,
  getRefreshToken,
  getStoredTokens,
  hasAuthTokens,
  saveAuthTokens,
  saveTokens,
} from "./authStorage";

describe("authStorage", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("stores and reads auth tokens", () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    expect(getAccessToken()).toBe("access-token");
    expect(getRefreshToken()).toBe("refresh-token");
    expect(hasAuthTokens()).toBe(true);
    expect(getStoredTokens()).toEqual({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });
  });

  it("returns null when stored tokens are incomplete", () => {
    localStorage.setItem("autolog.accessToken", "access-token");

    expect(hasAuthTokens()).toBe(false);
    expect(getStoredTokens()).toBeNull();
  });

  it("clears tokens using both public aliases", () => {
    saveTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    clearTokens();

    expect(getStoredTokens()).toBeNull();

    saveAuthTokens({
      accessToken: "next-access-token",
      refreshToken: "next-refresh-token",
    });

    clearAuthTokens();

    expect(hasAuthTokens()).toBe(false);
  });
});
