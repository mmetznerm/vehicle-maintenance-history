import { useState } from "react";
import { ArrowLeftIcon } from "../components/Icons";
import { MaintenanceForm } from "../components/MaintenanceForm";
import {
  maintenanceFieldLabels,
  mapMaintenanceFormValuesToRequest,
  type MaintenanceFormErrors,
  type MaintenanceFormValues,
} from "../components/maintenanceFormModel";
import { ApiError, createMaintenance } from "../services/api";

function getVehicleIdFromPath(pathname: string) {
  const match = pathname.match(/^\/vehicles\/([^/]+)\/maintenances\/new$/);

  return match?.[1] ? decodeURIComponent(match[1]) : "";
}

function getMaintenanceCreateErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return "";
    }

    if (error.status === 404) {
      return "Vehicle not found.";
    }

    if (error.status >= 500) {
      return "Could not connect to the server. Check whether the API is running.";
    }

    return error.message || "Could not save the maintenance record.";
  }

  return "Could not save the maintenance record.";
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
  const [errorMessage, setErrorMessage] = useState("");
  const [fieldErrors, setFieldErrors] = useState<MaintenanceFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const cancelHref = vehicleId ? `/vehicles/${vehicleId}` : "/vehicles";

  async function handleSubmit(values: MaintenanceFormValues) {
    setErrorMessage("");
    setFieldErrors({});

    if (!vehicleId) {
      setErrorMessage("Vehicle not found.");
      return;
    }

    setIsSubmitting(true);

    try {
      await createMaintenance(vehicleId, mapMaintenanceFormValuesToRequest(values));
      window.location.assign(`/vehicles/${vehicleId}`);
    } catch (error) {
      setFieldErrors(mapApiFieldErrors(error));
      setErrorMessage(getMaintenanceCreateErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="maintenance-create-page" aria-labelledby="maintenance-create-title">
      <div className="maintenance-create-shell">
        <header className="maintenance-create-header">
          <a className="maintenance-back-button" href={cancelHref} aria-label="Back to vehicle details">
            <ArrowLeftIcon aria-hidden />
          </a>
          <h1 id="maintenance-create-title">Add maintenance</h1>
          <span aria-hidden />
        </header>

        <section className="maintenance-create-card">
          <MaintenanceForm
            errorMessage={errorMessage}
            fieldErrors={fieldErrors}
            isSubmitting={isSubmitting}
            cancelHref={cancelHref}
            onFieldChange={() => {
              setErrorMessage("");
              setFieldErrors({});
            }}
            onSubmit={handleSubmit}
          />
        </section>
      </div>
    </main>
  );
}
