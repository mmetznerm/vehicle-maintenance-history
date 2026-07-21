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
  plate: "License plate",
  brand: "Brand",
  model: "Model",
  manufactureYear: "Manufacture year",
  color: "Color",
};

export function normalizePlate(value: string) {
  return value.toUpperCase().replace(/\s/g, "");
}

export function validateVehicle(values: VehicleFormValues) {
  const errors: VehicleFormErrors = {};
  const year = Number(values.manufactureYear);

  if (!values.plate.trim()) {
    errors.plate = "Enter the license plate.";
  } else if (normalizePlate(values.plate).length > 10) {
    errors.plate = "License plate must be no more than 10 characters.";
  }

  if (!values.brand.trim()) {
    errors.brand = "Enter the brand.";
  }

  if (!values.model.trim()) {
    errors.model = "Enter the model.";
  }

  if (!values.manufactureYear.trim()) {
    errors.manufactureYear = "Enter the manufacture year.";
  } else if (!Number.isInteger(year) || year < 1886 || year > 2100) {
    errors.manufactureYear = "Enter a valid year between 1886 and 2100.";
  }

  if (values.color.trim().length > 40) {
    errors.color = "Color must be no more than 40 characters.";
  }

  return errors;
}
