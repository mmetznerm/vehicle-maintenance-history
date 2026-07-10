export type VehicleSummary = {
  id: string;
  plate: string;
  brand: string;
  model: string;
  manufactureYear: number;
  color: string;
};

export type CreateVehicleRequest = {
  plate: string;
  brand: string;
  model: string;
  manufactureYear: number;
  color: string;
};

export type Vehicle = VehicleSummary & {
  createdAt?: string;
  updatedAt?: string;
};
