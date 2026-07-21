import { FormEvent, useMemo, useState } from "react";
import {
  BadgeIcon,
  BrandIcon,
  CalendarIcon,
  GaugeIcon,
  PaletteIcon,
  SaveIcon,
} from "./Icons";
import { TextField } from "./TextField";
import {
  emptyVehicleFormValues,
  normalizePlate,
  validateVehicle,
  vehicleFieldLabels,
  type VehicleFormErrors,
  type VehicleFormValues,
} from "./vehicleFormModel";

type VehicleFormProps = {
  initialValues?: VehicleFormValues;
  errorMessage?: string;
  fieldErrors?: VehicleFormErrors;
  submitLabel?: string;
  submittingLabel?: string;
  isSubmitting?: boolean;
  cancelHref: string;
  onSubmit: (values: VehicleFormValues) => void;
};

export function VehicleForm({
  initialValues = emptyVehicleFormValues,
  errorMessage = "",
  fieldErrors: apiFieldErrors = {},
  submitLabel = "Save",
  submittingLabel = "Saving...",
  isSubmitting = false,
  cancelHref,
  onSubmit,
}: VehicleFormProps) {
  const [values, setValues] = useState<VehicleFormValues>(initialValues);
  const [validationErrors, setValidationErrors] = useState<VehicleFormErrors>({});
  const [validationMessage, setValidationMessage] = useState("");
  const requiredText = useMemo(() => "* Required", []);
  const visibleErrorMessage = validationMessage || errorMessage;

  function updateField(field: keyof VehicleFormValues, value: string) {
    setValues((currentValues) => ({
      ...currentValues,
      [field]: field === "plate" ? normalizePlate(value) : value,
    }));
    setValidationErrors((currentErrors) => ({
      ...currentErrors,
      [field]: undefined,
    }));
    setValidationMessage("");
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setValidationMessage("");

    const validationErrors = validateVehicle(values);

    if (Object.keys(validationErrors).length > 0) {
      setValidationErrors(validationErrors);
      setValidationMessage("Review the highlighted fields before saving.");
      return;
    }

    onSubmit(values);
  }

  return (
    <form className="vehicle-form" onSubmit={handleSubmit} noValidate>
      <div className="vehicle-required-row">
        <span>{requiredText}</span>
      </div>

      <TextField
        id="plate"
        label={vehicleFieldLabels.plate}
        value={values.plate}
        placeholder="AAA-1234"
        autoComplete="off"
        required
        errorMessage={validationErrors.plate || apiFieldErrors.plate}
        leadingIcon={<BadgeIcon aria-hidden />}
        onChange={(value) => updateField("plate", value)}
      />

      <div className="vehicle-form-grid">
        <TextField
          id="brand"
          label={vehicleFieldLabels.brand}
          value={values.brand}
          placeholder="e.g., Toyota"
          autoComplete="organization"
          required
          errorMessage={validationErrors.brand || apiFieldErrors.brand}
          leadingIcon={<BrandIcon aria-hidden />}
          onChange={(value) => updateField("brand", value)}
        />

        <TextField
          id="model"
          label={vehicleFieldLabels.model}
          value={values.model}
          placeholder="e.g., Corolla"
          autoComplete="off"
          required
          errorMessage={validationErrors.model || apiFieldErrors.model}
          leadingIcon={<GaugeIcon aria-hidden />}
          onChange={(value) => updateField("model", value)}
        />

        <TextField
          id="manufactureYear"
          label="Year"
          type="number"
          value={values.manufactureYear}
          placeholder="YYYY"
          autoComplete="off"
          required
          errorMessage={validationErrors.manufactureYear || apiFieldErrors.manufactureYear}
          leadingIcon={<CalendarIcon aria-hidden />}
          onChange={(value) => updateField("manufactureYear", value)}
        />

        <TextField
          id="color"
          label={vehicleFieldLabels.color}
          value={values.color}
          placeholder="e.g., Silver"
          autoComplete="off"
          errorMessage={validationErrors.color || apiFieldErrors.color}
          leadingIcon={<PaletteIcon aria-hidden />}
          onChange={(value) => updateField("color", value)}
        />
      </div>

      {visibleErrorMessage ? (
        <p className="form-error vehicle-form-error" role="alert">
          {visibleErrorMessage}
        </p>
      ) : null}

      <footer className="vehicle-form-actions">
        <a className="text-button" href={cancelHref}>
          Cancel
        </a>
        <button className="primary-button vehicle-save-button" type="submit" disabled={isSubmitting}>
          <SaveIcon aria-hidden />
          <span>{isSubmitting ? submittingLabel : submitLabel}</span>
        </button>
      </footer>
    </form>
  );
}
