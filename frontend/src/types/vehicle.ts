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

export type UpdateVehicleRequest = CreateVehicleRequest;

export type Vehicle = VehicleSummary & {
  createdAt?: string;
  updatedAt?: string;
};

export type VehicleHistorySharing = {
  enabled: boolean;
  publicId: string | null;
};

export type PublicMaintenance = {
  id: string;
  maintenanceDate: string;
  odometer: number;
  description: string;
};

export type PublicVehicleHistory = {
  publicId: string;
  brand: string;
  model: string;
  manufactureYear: number;
  color: string | null;
  maintenances: PublicMaintenance[];
};
