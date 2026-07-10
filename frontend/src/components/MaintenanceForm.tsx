import { FormEvent, useState } from "react";
import { CalendarIcon, GaugeIcon, MoneyIcon } from "./Icons";
import {
  emptyMaintenanceFormValues,
  maintenanceFieldLabels,
  validateMaintenance,
  type MaintenanceFormErrors,
  type MaintenanceFormValues,
} from "./maintenanceFormModel";

type MaintenanceFormProps = {
  initialValues?: MaintenanceFormValues;
  errorMessage?: string;
  fieldErrors?: MaintenanceFormErrors;
  isSubmitting?: boolean;
  cancelHref: string;
  onFieldChange?: () => void;
  onSubmit: (values: MaintenanceFormValues) => void;
};

export function MaintenanceForm({
  initialValues = emptyMaintenanceFormValues(),
  errorMessage = "",
  fieldErrors: apiFieldErrors = {},
  isSubmitting = false,
  cancelHref,
  onFieldChange,
  onSubmit,
}: MaintenanceFormProps) {
  const [values, setValues] = useState<MaintenanceFormValues>(initialValues);
  const [validationErrors, setValidationErrors] = useState<MaintenanceFormErrors>({});
  const [validationMessage, setValidationMessage] = useState("");
  const visibleErrorMessage = validationMessage || errorMessage;

  function updateField(field: keyof MaintenanceFormValues, value: string) {
    setValues((currentValues) => ({
      ...currentValues,
      [field]: value,
    }));
    setValidationErrors((currentErrors) => ({
      ...currentErrors,
      [field]: undefined,
    }));
    setValidationMessage("");
    onFieldChange?.();
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setValidationMessage("");

    const validationErrors = validateMaintenance(values);

    if (Object.keys(validationErrors).length > 0) {
      setValidationErrors(validationErrors);
      setValidationMessage("Revise os campos destacados antes de salvar.");
      return;
    }

    onSubmit(values);
  }

  return (
    <form className="maintenance-create-form" onSubmit={handleSubmit} noValidate>
      <div className="field-group">
        <label className="field-label" htmlFor="maintenanceDate">
          {maintenanceFieldLabels.maintenanceDate}
        </label>
        <div
          className={`field-control maintenance-field-control${
            validationErrors.maintenanceDate || apiFieldErrors.maintenanceDate ? " has-error" : ""
          }`}
        >
          <span className="field-icon">
            <CalendarIcon aria-hidden />
          </span>
          <input
            id="maintenanceDate"
            className="field-input"
            type="date"
            value={values.maintenanceDate}
            required
            aria-invalid={validationErrors.maintenanceDate || apiFieldErrors.maintenanceDate ? "true" : undefined}
            aria-describedby={
              validationErrors.maintenanceDate || apiFieldErrors.maintenanceDate
                ? "maintenanceDate-error"
                : undefined
            }
            onChange={(event) => updateField("maintenanceDate", event.target.value)}
          />
        </div>
        {validationErrors.maintenanceDate || apiFieldErrors.maintenanceDate ? (
          <p className="field-error" id="maintenanceDate-error" role="alert">
            {validationErrors.maintenanceDate || apiFieldErrors.maintenanceDate}
          </p>
        ) : null}
      </div>

      <div className="field-group">
        <label className="field-label" htmlFor="odometer">
          {maintenanceFieldLabels.odometer}
        </label>
        <div
          className={`field-control maintenance-field-control${
            validationErrors.odometer || apiFieldErrors.odometer ? " has-error" : ""
          }`}
        >
          <span className="field-icon">
            <GaugeIcon aria-hidden />
          </span>
          <input
            id="odometer"
            className="field-input"
            type="number"
            min="0"
            step="1"
            value={values.odometer}
            placeholder="ex.: 45000"
            required
            aria-invalid={validationErrors.odometer || apiFieldErrors.odometer ? "true" : undefined}
            aria-describedby={validationErrors.odometer || apiFieldErrors.odometer ? "odometer-error" : undefined}
            onChange={(event) => updateField("odometer", event.target.value)}
          />
          <span className="field-affix">km</span>
        </div>
        {validationErrors.odometer || apiFieldErrors.odometer ? (
          <p className="field-error" id="odometer-error" role="alert">
            {validationErrors.odometer || apiFieldErrors.odometer}
          </p>
        ) : null}
      </div>

      <div className="field-group">
        <label className="field-label" htmlFor="cost">
          {maintenanceFieldLabels.cost}
        </label>
        <div
          className={`field-control maintenance-field-control${
            validationErrors.cost || apiFieldErrors.cost ? " has-error" : ""
          }`}
        >
          <span className="field-icon">
            <MoneyIcon aria-hidden />
          </span>
          <input
            id="cost"
            className="field-input"
            type="text"
            inputMode="decimal"
            value={values.cost}
            placeholder="0,00"
            required
            aria-invalid={validationErrors.cost || apiFieldErrors.cost ? "true" : undefined}
            aria-describedby={validationErrors.cost || apiFieldErrors.cost ? "cost-error" : undefined}
            onChange={(event) => updateField("cost", event.target.value)}
          />
          <span className="field-affix">BRL</span>
        </div>
        {validationErrors.cost || apiFieldErrors.cost ? (
          <p className="field-error" id="cost-error" role="alert">
            {validationErrors.cost || apiFieldErrors.cost}
          </p>
        ) : null}
      </div>

      <div className="field-group">
        <label className="field-label" htmlFor="description">
          {maintenanceFieldLabels.description}
        </label>
        <div
          className={`field-control maintenance-textarea-control${
            validationErrors.description || apiFieldErrors.description ? " has-error" : ""
          }`}
        >
          <textarea
            id="description"
            className="maintenance-textarea"
            value={values.description}
            maxLength={500}
            placeholder="Troca de óleo, pastilhas de freio etc."
            required
            aria-invalid={validationErrors.description || apiFieldErrors.description ? "true" : undefined}
            aria-describedby={
              validationErrors.description || apiFieldErrors.description ? "description-error" : undefined
            }
            onChange={(event) => updateField("description", event.target.value)}
          />
        </div>
        {validationErrors.description || apiFieldErrors.description ? (
          <p className="field-error" id="description-error" role="alert">
            {validationErrors.description || apiFieldErrors.description}
          </p>
        ) : null}
      </div>

      {visibleErrorMessage ? (
        <p className="form-error maintenance-form-error" role="alert">
          {visibleErrorMessage}
        </p>
      ) : null}

      <footer className="maintenance-create-actions">
        <a className="text-button" href={cancelHref}>
          Cancelar
        </a>
        <button className="primary-button maintenance-save-button" type="submit" disabled={isSubmitting}>
          <span>{isSubmitting ? "Salvando..." : "Salvar"}</span>
        </button>
      </footer>
    </form>
  );
}
