import type { CreateMaintenanceRequest, Maintenance } from "../types/maintenance";

export type MaintenanceFormValues = {
  maintenanceDate: string;
  odometer: string;
  cost: string;
  description: string;
};

export type MaintenanceFormErrors = Partial<Record<keyof MaintenanceFormValues, string>>;

export const maintenanceFieldLabels: Record<keyof MaintenanceFormValues, string> = {
  maintenanceDate: "Data da manutenção",
  odometer: "Odômetro",
  cost: "Custo total",
  description: "Descrição do serviço",
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
    errors.maintenanceDate = "Informe a data da manutenção.";
  }

  if (!values.odometer.trim()) {
    errors.odometer = "Informe o odômetro.";
  } else if (!Number.isInteger(odometer) || odometer < 0) {
    errors.odometer = "Informe um odômetro válido.";
  }

  if (!values.cost.trim()) {
    errors.cost = "Informe o custo total.";
  } else if (!/^\d+([.,]\d{1,2})?$/.test(values.cost.trim()) || Number.isNaN(cost) || cost < 0) {
    errors.cost = "Informe um custo válido com até 2 casas decimais.";
  }

  if (!values.description.trim()) {
    errors.description = "Informe a descrição do serviço.";
  } else if (values.description.trim().length > 500) {
    errors.description = "Use no máximo 500 caracteres.";
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
