import { useEffect, useState } from "react";
import { ArrowLeftIcon } from "../components/Icons";
import { MaintenanceForm } from "../components/MaintenanceForm";
import {
  maintenanceFieldLabels,
  mapMaintenanceFormValuesToRequest,
  mapMaintenanceToFormValues,
  type MaintenanceFormErrors,
  type MaintenanceFormValues,
} from "../components/maintenanceFormModel";
import { ApiError, getMaintenance, updateMaintenance } from "../services/api";

function getMaintenanceRouteParams(pathname: string) {
  const match = pathname.match(/^\/vehicles\/([^/]+)\/maintenances\/([^/]+)\/edit$/);

  return {
    vehicleId: match?.[1] ? decodeURIComponent(match[1]) : "",
    maintenanceId: match?.[2] ? decodeURIComponent(match[2]) : "",
  };
}

function getMaintenanceLoadErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Maintenance record not found.";
    }

    if (error.status === 401) {
      return "";
    }

    if (error.status >= 500) {
      return "Could not connect to the server. Check whether the API is running.";
    }

    return error.message || "Could not load the maintenance record.";
  }

  return "Could not load the maintenance record.";
}

function getMaintenanceUpdateErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Maintenance record not found.";
    }

    if (error.status === 401) {
      return "";
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

export function MaintenanceEditPage() {
  const { vehicleId, maintenanceId } = getMaintenanceRouteParams(window.location.pathname);
  const [initialValues, setInitialValues] = useState<MaintenanceFormValues | null>(null);
  const [fieldErrors, setFieldErrors] = useState<MaintenanceFormErrors>({});
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const cancelHref = vehicleId ? `/vehicles/${vehicleId}` : "/vehicles";

  useEffect(() => {
    let isMounted = true;

    async function loadMaintenance() {
      if (!vehicleId || !maintenanceId) {
        setErrorMessage("Maintenance record not found.");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setErrorMessage("");

      try {
        const maintenance = await getMaintenance(vehicleId, maintenanceId);

        if (isMounted) {
          setInitialValues(mapMaintenanceToFormValues(maintenance));
        }
      } catch (error) {
        if (isMounted) {
          setErrorMessage(getMaintenanceLoadErrorMessage(error));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadMaintenance();

    return () => {
      isMounted = false;
    };
  }, [vehicleId, maintenanceId]);

  async function handleSubmit(values: MaintenanceFormValues) {
    setErrorMessage("");
    setFieldErrors({});

    if (!vehicleId || !maintenanceId) {
      setErrorMessage("Maintenance record not found.");
      return;
    }

    setIsSubmitting(true);

    try {
      await updateMaintenance(vehicleId, maintenanceId, mapMaintenanceFormValuesToRequest(values));
      window.location.assign(`/vehicles/${vehicleId}`);
    } catch (error) {
      setFieldErrors(mapApiFieldErrors(error));
      setErrorMessage(getMaintenanceUpdateErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="maintenance-create-page" aria-labelledby="maintenance-edit-title">
      <div className="maintenance-create-shell">
        <header className="maintenance-create-header">
          <a className="maintenance-back-button" href={cancelHref} aria-label="Back to vehicle details">
            <ArrowLeftIcon aria-hidden />
          </a>
          <h1 id="maintenance-edit-title">Edit maintenance</h1>
          <span aria-hidden />
        </header>

        <section className="maintenance-create-card">
          {isLoading ? (
            <div className="vehicle-form-status" role="status" aria-live="polite">
              <span className="loading-spinner" aria-hidden />
              <p>Loading maintenance...</p>
            </div>
          ) : null}

          {!isLoading && errorMessage && !initialValues ? (
            <div className="vehicle-form-status error-status" role="alert">
              <p>{errorMessage}</p>
              <a className="secondary-button vehicle-status-action" href={cancelHref}>
                Back to vehicle
              </a>
            </div>
          ) : null}

          {!isLoading && initialValues ? (
            <MaintenanceForm
              initialValues={initialValues}
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
          ) : null}
        </section>
      </div>
    </main>
  );
}
