import { FormEvent, useMemo, useState } from "react";
import {
  ArrowLeftIcon,
  BadgeIcon,
  BrandIcon,
  CalendarIcon,
  CarIcon,
  GaugeIcon,
  PaletteIcon,
  SaveIcon,
} from "../components/Icons";
import { TextField } from "../components/TextField";
import { ApiError, createVehicle } from "../services/api";

type VehicleFormValues = {
  plate: string;
  brand: string;
  model: string;
  manufactureYear: string;
  color: string;
};

type VehicleFormErrors = Partial<Record<keyof VehicleFormValues, string>>;

const initialValues: VehicleFormValues = {
  plate: "",
  brand: "",
  model: "",
  manufactureYear: "",
  color: "",
};

const fieldLabels: Record<keyof VehicleFormValues, string> = {
  plate: "Placa",
  brand: "Marca",
  model: "Modelo",
  manufactureYear: "Ano de fabricação",
  color: "Cor",
};

function normalizePlate(value: string) {
  return value.toUpperCase().replace(/\s/g, "");
}

function validateVehicle(values: VehicleFormValues) {
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

function getVehicleCreateErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return "";
    }

    if (error.status >= 500) {
      return "Não foi possível conectar ao servidor. Verifique se a API está em execução.";
    }

    return error.message || "Não foi possível salvar o veículo.";
  }

  return "Não foi possível salvar o veículo agora.";
}

function mapApiFieldErrors(error: unknown) {
  const errors: VehicleFormErrors = {};

  if (!(error instanceof ApiError)) {
    return errors;
  }

  error.fieldErrors.forEach((fieldError) => {
    const field = fieldError.field as keyof VehicleFormValues;

    if (field in fieldLabels) {
      errors[field] = fieldError.message;
    }
  });

  return errors;
}

export function VehicleCreatePage() {
  const [values, setValues] = useState<VehicleFormValues>(initialValues);
  const [fieldErrors, setFieldErrors] = useState<VehicleFormErrors>({});
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const requiredText = useMemo(() => "* Obrigatório", []);

  function updateField(field: keyof VehicleFormValues, value: string) {
    setValues((currentValues) => ({
      ...currentValues,
      [field]: field === "plate" ? normalizePlate(value) : value,
    }));
    setFieldErrors((currentErrors) => ({
      ...currentErrors,
      [field]: undefined,
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage("");

    const validationErrors = validateVehicle(values);

    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setErrorMessage("Revise os campos destacados antes de salvar.");
      return;
    }

    setIsSubmitting(true);

    try {
      await createVehicle({
        plate: normalizePlate(values.plate),
        brand: values.brand.trim(),
        model: values.model.trim(),
        manufactureYear: Number(values.manufactureYear),
        color: values.color.trim(),
      });

      window.location.assign("/vehicles");
    } catch (error) {
      const apiFieldErrors = mapApiFieldErrors(error);
      setFieldErrors(apiFieldErrors);
      setErrorMessage(getVehicleCreateErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="vehicle-form-page" aria-labelledby="vehicle-form-title">
      <div className="vehicle-form-shell">
        <a className="back-link" href="/vehicles">
          <ArrowLeftIcon aria-hidden />
          <span>Voltar para veículos</span>
        </a>

        <section className="vehicle-form-card">
          <header className="vehicle-form-header">
            <span className="vehicle-form-icon" aria-hidden>
              <CarIcon />
            </span>
            <div>
              <h1 id="vehicle-form-title">Detalhes do veículo</h1>
              <p>Adicione um novo veículo à sua frota ou atualize as informações existentes.</p>
            </div>
          </header>

          <form className="vehicle-form" onSubmit={handleSubmit} noValidate>
            <div className="vehicle-required-row">
              <span>{requiredText}</span>
            </div>

            <TextField
              id="plate"
              label="Placa"
              value={values.plate}
              placeholder="AAA-1234"
              autoComplete="off"
              required
              errorMessage={fieldErrors.plate}
              leadingIcon={<BadgeIcon aria-hidden />}
              onChange={(value) => updateField("plate", value)}
            />

            <div className="vehicle-form-grid">
              <TextField
                id="brand"
                label="Marca"
                value={values.brand}
                placeholder="ex.: Toyota"
                autoComplete="organization"
                required
                errorMessage={fieldErrors.brand}
                leadingIcon={<BrandIcon aria-hidden />}
                onChange={(value) => updateField("brand", value)}
              />

              <TextField
                id="model"
                label="Modelo"
                value={values.model}
                placeholder="ex.: Corolla"
                autoComplete="off"
                required
                errorMessage={fieldErrors.model}
                leadingIcon={<GaugeIcon aria-hidden />}
                onChange={(value) => updateField("model", value)}
              />

              <TextField
                id="manufactureYear"
                label="Ano"
                type="number"
                value={values.manufactureYear}
                placeholder="AAAA"
                autoComplete="off"
                required
                errorMessage={fieldErrors.manufactureYear}
                leadingIcon={<CalendarIcon aria-hidden />}
                onChange={(value) => updateField("manufactureYear", value)}
              />

              <TextField
                id="color"
                label="Cor"
                value={values.color}
                placeholder="ex.: Prata"
                autoComplete="off"
                errorMessage={fieldErrors.color}
                leadingIcon={<PaletteIcon aria-hidden />}
                onChange={(value) => updateField("color", value)}
              />
            </div>

            {errorMessage ? (
              <p className="form-error vehicle-form-error" role="alert">
                {errorMessage}
              </p>
            ) : null}

            <footer className="vehicle-form-actions">
              <a className="text-button" href="/vehicles">
                Cancelar
              </a>
              <button className="primary-button vehicle-save-button" type="submit" disabled={isSubmitting}>
                <SaveIcon aria-hidden />
                <span>{isSubmitting ? "Salvando..." : "Salvar"}</span>
              </button>
            </footer>
          </form>
        </section>
      </div>
    </main>
  );
}
