import type { CreateMaintenanceRequest, Maintenance } from "../types/maintenance";

export type MaintenanceFormValues = {
  maintenanceDate: string;
  odometer: string;
  cost: string;
  description: string;
};

export type MaintenanceFormErrors = Partial<Record<keyof MaintenanceFormValues, string>>;

export const maintenanceFieldLabels: Record<keyof MaintenanceFormValues, string> = {
  maintenanceDate: "Maintenance date",
  odometer: "Odometer",
  cost: "Total cost",
  description: "Service description",
};

export function getTodayDateInputValue() {
  const today = new Date();
  const timezoneOffset = today.getTimezoneOffset() * 60_000;

  return new Date(today.getTime() - timezoneOffset).toISOString().slice(0, 10);
}

export function emptyMaintenanceFormValues(): MaintenanceFormValues {
  return {
    maintenanceDate: getTodayDateInputValue(),
    odometer: "",
    cost: "",
    description: "",
  };
}

export function normalizeDecimalValue(value: string) {
  return value.trim().replace(",", ".");
}

export function validateMaintenance(values: MaintenanceFormValues) {
  const errors: MaintenanceFormErrors = {};
  const odometer = Number(values.odometer);
  const normalizedCost = normalizeDecimalValue(values.cost);
  const cost = Number(normalizedCost);

  if (!values.maintenanceDate) {
    errors.maintenanceDate = "Enter the maintenance date.";
  }

  if (!values.odometer.trim()) {
    errors.odometer = "Enter the odometer reading.";
  } else if (!Number.isInteger(odometer) || odometer < 0) {
    errors.odometer = "Enter a valid odometer reading.";
  }

  if (!values.cost.trim()) {
    errors.cost = "Enter the total cost.";
  } else if (!/^\d+([.,]\d{1,2})?$/.test(values.cost.trim()) || Number.isNaN(cost) || cost < 0) {
    errors.cost = "Enter a valid cost with up to 2 decimal places.";
  }

  if (!values.description.trim()) {
    errors.description = "Enter the service description.";
  } else if (values.description.trim().length > 500) {
    errors.description = "Use no more than 500 characters.";
  }

  return errors;
}

export function mapMaintenanceToFormValues(maintenance: Maintenance): MaintenanceFormValues {
  return {
    maintenanceDate: maintenance.maintenanceDate,
    odometer: String(maintenance.odometer),
    cost: Number(maintenance.cost).toFixed(2),
    description: maintenance.description,
  };
}

export function mapMaintenanceFormValuesToRequest(
  values: MaintenanceFormValues,
): CreateMaintenanceRequest {
  return {
    maintenanceDate: values.maintenanceDate,
    odometer: Number(values.odometer),
    description: values.description.trim(),
    cost: Number(normalizeDecimalValue(values.cost)),
  };
}
