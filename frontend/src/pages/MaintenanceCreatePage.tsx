import { FormEvent, useState } from "react";
import { ArrowLeftIcon, CalendarIcon, GaugeIcon, MoneyIcon } from "../components/Icons";
import { ApiError, createMaintenance } from "../services/api";

type MaintenanceFormValues = {
  maintenanceDate: string;
  odometer: string;
  cost: string;
  description: string;
};

type MaintenanceFormErrors = Partial<Record<keyof MaintenanceFormValues, string>>;

const maintenanceFieldLabels: Record<keyof MaintenanceFormValues, string> = {
  maintenanceDate: "Data da manutenção",
  odometer: "Odômetro",
  cost: "Custo total",
  description: "Descrição do serviço",
};

function getVehicleIdFromPath(pathname: string) {
  const match = pathname.match(/^\/vehicles\/([^/]+)\/maintenances\/new$/);

  return match?.[1] ? decodeURIComponent(match[1]) : "";
}

function getTodayDateInputValue() {
  const today = new Date();
  const timezoneOffset = today.getTimezoneOffset() * 60_000;

  return new Date(today.getTime() - timezoneOffset).toISOString().slice(0, 10);
}

function emptyMaintenanceFormValues(): MaintenanceFormValues {
  return {
    maintenanceDate: getTodayDateInputValue(),
    odometer: "",
    cost: "",
    description: "",
  };
}

function normalizeDecimalValue(value: string) {
  return value.trim().replace(",", ".");
}

function validateMaintenance(values: MaintenanceFormValues) {
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

function getMaintenanceCreateErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return "";
    }

    if (error.status === 404) {
      return "Veículo não encontrado.";
    }

    if (error.status >= 500) {
      return "Não foi possível conectar ao servidor. Verifique se a API está em execução.";
    }

    return error.message || "Não foi possível salvar a manutenção.";
  }

  return "Não foi possível salvar a manutenção agora.";
}

function mapApiFieldErrors(error: unknown) {
  const fieldErrors: MaintenanceFormErrors = {};

  if (!(error instanceof ApiError)) {
    return fieldErrors;
  }

  error.fieldErrors.forEach((fieldError) => {
    const field = fieldError.field as keyof MaintenanceFormValues;

    if (field in maintenanceFieldLabels) {
      fieldErrors[field] = fieldError.message;
    }
  });

  return fieldErrors;
}

export function MaintenanceCreatePage() {
  const vehicleId = getVehicleIdFromPath(window.location.pathname);
  const [values, setValues] = useState<MaintenanceFormValues>(() => emptyMaintenanceFormValues());
  const [validationErrors, setValidationErrors] = useState<MaintenanceFormErrors>({});
  const [apiFieldErrors, setApiFieldErrors] = useState<MaintenanceFormErrors>({});
  const [errorMessage, setErrorMessage] = useState("");
  const [validationMessage, setValidationMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const cancelHref = vehicleId ? `/vehicles/${vehicleId}` : "/vehicles";
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
    setApiFieldErrors((currentErrors) => ({
      ...currentErrors,
      [field]: undefined,
    }));
    setValidationMessage("");
    setErrorMessage("");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setValidationMessage("");
    setErrorMessage("");
    setApiFieldErrors({});

    if (!vehicleId) {
      setErrorMessage("Veículo não encontrado.");
      return;
    }

    const validationErrors = validateMaintenance(values);

    if (Object.keys(validationErrors).length > 0) {
      setValidationErrors(validationErrors);
      setValidationMessage("Revise os campos destacados antes de salvar.");
      return;
    }

    setIsSubmitting(true);

    try {
      await createMaintenance(vehicleId, {
        maintenanceDate: values.maintenanceDate,
        odometer: Number(values.odometer),
        description: values.description.trim(),
        cost: Number(normalizeDecimalValue(values.cost)),
      });

      window.location.assign(`/vehicles/${vehicleId}`);
    } catch (error) {
      setApiFieldErrors(mapApiFieldErrors(error));
      setErrorMessage(getMaintenanceCreateErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="maintenance-create-page" aria-labelledby="maintenance-create-title">
      <div className="maintenance-create-shell">
        <header className="maintenance-create-header">
          <a className="maintenance-back-button" href={cancelHref} aria-label="Voltar para detalhes do veículo">
            <ArrowLeftIcon aria-hidden />
          </a>
          <h1 id="maintenance-create-title">Cadastrar manutenção</h1>
          <span aria-hidden />
        </header>

        <section className="maintenance-create-card">
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
                  aria-invalid={
                    validationErrors.maintenanceDate || apiFieldErrors.maintenanceDate ? "true" : undefined
                  }
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
                  aria-describedby={
                    validationErrors.odometer || apiFieldErrors.odometer ? "odometer-error" : undefined
                  }
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
                  aria-invalid={
                    validationErrors.description || apiFieldErrors.description ? "true" : undefined
                  }
                  aria-describedby={
                    validationErrors.description || apiFieldErrors.description
                      ? "description-error"
                      : undefined
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
        </section>
      </div>
    </main>
  );
}
