export type LoginRequest = {
  emailOrPhone: string;
  password: string;
};

export type AuthTokensResponse = {
  accessToken: string;
  refreshToken: string;
};
