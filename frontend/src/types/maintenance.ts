export type Maintenance = {
  id: string;
  vehicleId: string;
  maintenanceDate: string;
  odometer: number;
  description: string;
  cost: number;
  createdAt?: string;
  updatedAt?: string;
};

export type CreateMaintenanceRequest = {
  maintenanceDate: string;
  odometer: number;
  description: string;
  cost: number;
};
