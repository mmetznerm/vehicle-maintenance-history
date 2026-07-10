export type VehicleFormValues = {
  plate: string;
  brand: string;
  model: string;
  manufactureYear: string;
  color: string;
};

export type VehicleFormErrors = Partial<Record<keyof VehicleFormValues, string>>;

export const emptyVehicleFormValues: VehicleFormValues = {
  plate: "",
  brand: "",
  model: "",
  manufactureYear: "",
  color: "",
};

export const vehicleFieldLabels: Record<keyof VehicleFormValues, string> = {
  plate: "Placa",
  brand: "Marca",
  model: "Modelo",
  manufactureYear: "Ano de fabricação",
  color: "Cor",
};

export function normalizePlate(value: string) {
  return value.toUpperCase().replace(/\s/g, "");
}

export function validateVehicle(values: VehicleFormValues) {
  const errors: VehicleFormErrors = {};
  const year = Number(values.manufactureYear);

  if (!values.plate.trim()) {
    errors.plate = "Informe a placa.";
  } else if (normalizePlate(values.plate).length > 10) {
    errors.plate = "A placa deve ter no máximo 10 caracteres.";
  }

  if (!values.brand.trim()) {
    errors.brand = "Informe a marca.";
  }

  if (!values.model.trim()) {
    errors.model = "Informe o modelo.";
  }

  if (!values.manufactureYear.trim()) {
    errors.manufactureYear = "Informe o ano de fabricação.";
  } else if (!Number.isInteger(year) || year < 1886 || year > 2100) {
    errors.manufactureYear = "Informe um ano válido entre 1886 e 2100.";
  }

  if (values.color.trim().length > 40) {
    errors.color = "A cor deve ter no máximo 40 caracteres.";
  }

  return errors;
}
