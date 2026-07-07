export {};

declare global {
  interface Window {
    __AUTOLOG_CONFIG__?: {
      VITE_API_BASE_URL?: string;
    };
  }
}
