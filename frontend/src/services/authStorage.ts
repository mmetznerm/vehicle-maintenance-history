import type { AuthTokensResponse } from "../types/auth";

const ACCESS_TOKEN_KEY = "autolog.accessToken";
const REFRESH_TOKEN_KEY = "autolog.refreshToken";

export function saveAuthTokens(tokens: AuthTokensResponse) {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

type AccessTokenPayload = {
  fullName?: string;
  emailOrPhone?: string;
};

function decodeBase64Url(value: string) {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const paddedBase64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
  const binary = atob(paddedBase64);
  const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));

  return new TextDecoder().decode(bytes);
}

function readAccessTokenPayload(): AccessTokenPayload | null {
  const accessToken = getAccessToken();
  const payload = accessToken?.split(".")[1];

  if (!payload) {
    return null;
  }

  try {
    return JSON.parse(decodeBase64Url(payload)) as AccessTokenPayload;
  } catch {
    return null;
  }
}

export function getCurrentUserDisplayName() {
  const payload = readAccessTokenPayload();

  return payload?.fullName?.trim() || payload?.emailOrPhone?.trim() || "User";
}

export function hasAuthTokens() {
  return Boolean(getAccessToken() && getRefreshToken());
}

export function clearAuthTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function saveTokens(tokens: AuthTokensResponse) {
  saveAuthTokens(tokens);
}

export function getStoredTokens(): AuthTokensResponse | null {
  const accessToken = getAccessToken();
  const refreshToken = getRefreshToken();

  if (!accessToken || !refreshToken) {
    return null;
  }

  return {
    accessToken,
    refreshToken,
  };
}

export function clearTokens() {
  clearAuthTokens();
}
