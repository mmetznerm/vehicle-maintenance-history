import { useEffect, useState } from "react";
import { ArrowLeftIcon, CarIcon } from "../components/Icons";
import { VehicleForm } from "../components/VehicleForm";
import {
  normalizePlate,
  type VehicleFormErrors,
  type VehicleFormValues,
  vehicleFieldLabels,
} from "../components/vehicleFormModel";
import { ApiError, getVehicle, updateVehicle } from "../services/api";
import type { Vehicle } from "../types/vehicle";

function getVehicleIdFromPath(pathname: string) {
  const match = pathname.match(/^\/vehicles\/([^/]+)\/edit$/);

  return match?.[1] ? decodeURIComponent(match[1]) : "";
}

function mapVehicleToFormValues(vehicle: Vehicle): VehicleFormValues {
  return {
    plate: vehicle.plate,
    brand: vehicle.brand,
    model: vehicle.model,
    manufactureYear: String(vehicle.manufactureYear),
    color: vehicle.color ?? "",
  };
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

function getVehicleLoadErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Vehicle not found.";
    }

    if (error.status === 401) {
      return "";
    }

    if (error.status >= 500) {
      return "Could not connect to the server. Check whether the API is running.";
    }

    return error.message || "Could not load the vehicle data.";
  }

  return "Could not load the vehicle data.";
}

function getVehicleUpdateErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Vehicle not found.";
    }

    if (error.status === 401) {
      return "";
    }

    if (error.status >= 500) {
      return "Could not connect to the server. Check whether the API is running.";
    }

    return error.message || "Could not save the changes.";
  }

  return "Could not save the changes.";
}

export function VehicleEditPage() {
  const vehicleId = getVehicleIdFromPath(window.location.pathname);
  const [initialValues, setInitialValues] = useState<VehicleFormValues | null>(null);
  const [fieldErrors, setFieldErrors] = useState<VehicleFormErrors>({});
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let isMounted = true;

    async function loadVehicle() {
      if (!vehicleId) {
        setErrorMessage("Vehicle not found.");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setErrorMessage("");

      try {
        const vehicle = await getVehicle(vehicleId);

        if (isMounted) {
          setInitialValues(mapVehicleToFormValues(vehicle));
        }
      } catch (error) {
        if (isMounted) {
          setErrorMessage(getVehicleLoadErrorMessage(error));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadVehicle();

    return () => {
      isMounted = false;
    };
  }, [vehicleId]);

  async function handleSubmit(values: VehicleFormValues) {
    setErrorMessage("");
    setFieldErrors({});
    setIsSubmitting(true);

    try {
      const vehicle = await updateVehicle(vehicleId, {
        plate: normalizePlate(values.plate),
        brand: values.brand.trim(),
        model: values.model.trim(),
        manufactureYear: Number(values.manufactureYear),
        color: values.color.trim(),
      });

      window.location.assign(`/vehicles/${vehicle.id || vehicleId}`);
    } catch (error) {
      setFieldErrors(mapApiFieldErrors(error));
      setErrorMessage(getVehicleUpdateErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="vehicle-form-page" aria-labelledby="vehicle-form-title">
      <div className="vehicle-form-shell">
        <a className="back-link" href="/vehicles">
          <ArrowLeftIcon aria-hidden />
          <span>Back to vehicles</span>
        </a>

        <section className="vehicle-form-card">
          <header className="vehicle-form-header">
            <span className="vehicle-form-icon" aria-hidden>
              <CarIcon />
            </span>
            <div>
              <h1 id="vehicle-form-title">Edit vehicle</h1>
              <p>Update the selected vehicle's information.</p>
            </div>
          </header>

          {isLoading ? (
            <div className="vehicle-form-status" role="status" aria-live="polite">
              <span className="loading-spinner" aria-hidden />
              <p>Loading vehicle...</p>
            </div>
          ) : null}

          {!isLoading && errorMessage && !initialValues ? (
            <div className="vehicle-form-status error-status" role="alert">
              <p>{errorMessage}</p>
              <a className="secondary-button vehicle-status-action" href="/vehicles">
                Back to vehicles
              </a>
            </div>
          ) : null}

          {!isLoading && initialValues ? (
            <VehicleForm
              initialValues={initialValues}
              errorMessage={errorMessage}
              fieldErrors={fieldErrors}
              isSubmitting={isSubmitting}
              cancelHref="/vehicles"
              onSubmit={handleSubmit}
            />
          ) : null}
        </section>
      </div>
    </main>
  );
}
