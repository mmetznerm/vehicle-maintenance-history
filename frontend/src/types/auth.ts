export type LoginRequest = {
  emailOrPhone: string;
  password: string;
};

export type RegisterRequest = {
  fullName: string;
  emailOrPhone: string;
  password: string;
};

export type LogoutRequest = {
  refreshToken: string;
};

export type AuthTokensResponse = {
  accessToken: string;
  refreshToken: string;
};
