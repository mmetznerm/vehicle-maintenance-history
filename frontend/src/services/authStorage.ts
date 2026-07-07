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

export function hasAuthTokens() {
  return Boolean(getAccessToken() && getRefreshToken());
}

export function clearAuthTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}
