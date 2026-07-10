import { useState } from "react";
import { ArrowLeftIcon, CarIcon } from "../components/Icons";
import { VehicleForm } from "../components/VehicleForm";
import {
  normalizePlate,
  type VehicleFormErrors,
  type VehicleFormValues,
  vehicleFieldLabels,
} from "../components/vehicleFormModel";
import { ApiError, createVehicle } from "../services/api";

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
  const fieldErrors: VehicleFormErrors = {};

  if (!(error instanceof ApiError)) {
    return fieldErrors;
  }

  error.fieldErrors.forEach((fieldError) => {
    const field = fieldError.field as keyof VehicleFormValues;

    if (field in vehicleFieldLabels) {
      fieldErrors[field] = fieldError.message;
    }
  });

  return fieldErrors;
}

export function VehicleCreatePage() {
  const [errorMessage, setErrorMessage] = useState("");
  const [fieldErrors, setFieldErrors] = useState<VehicleFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(values: VehicleFormValues) {
    setErrorMessage("");
    setFieldErrors({});
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
      setFieldErrors(mapApiFieldErrors(error));
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

          <VehicleForm
            errorMessage={errorMessage}
            fieldErrors={fieldErrors}
            isSubmitting={isSubmitting}
            cancelHref="/vehicles"
            onSubmit={handleSubmit}
          />
        </section>
      </div>
    </main>
  );
}
