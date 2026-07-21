import { useEffect, useMemo, useState } from "react";
import { AppSidebar } from "../components/AppSidebar";
import { CalendarIcon, CarIcon, EditIcon, GaugeIcon, PlusIcon, TrashIcon } from "../components/Icons";
import { MaintenanceHistory } from "../components/MaintenanceHistory";
import { formatOdometer } from "../components/maintenanceFormat";
import {
  ApiError,
  deleteMaintenance,
  deleteVehicle,
  disableVehicleHistorySharing,
  enableVehicleHistorySharing,
  getVehicleHistorySharing,
  getVehicle,
  listMaintenances,
} from "../services/api";
import type { Maintenance } from "../types/maintenance";
import type { Vehicle, VehicleHistorySharing } from "../types/vehicle";

function getVehicleIdFromPath(pathname: string) {
  const match = pathname.match(/^\/vehicles\/([^/]+)$/);

  return match?.[1] ? decodeURIComponent(match[1]) : "";
}

function getVehicleDetailsErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Veículo não encontrado.";
    }

    if (error.status === 401) {
      return "";
    }

    if (error.status >= 500) {
      return "Não foi possível conectar ao servidor. Verifique se a API está em execução.";
    }

    return error.message || "Não foi possível carregar os detalhes do veículo.";
  }

  return "Não foi possível carregar os detalhes do veículo agora.";
}

type VehicleSummaryCardProps = {
  vehicle: Vehicle;
  currentOdometer: number | null;
  isDeleting: boolean;
  onDelete: () => void;
};

function VehicleSummaryCard({
  vehicle,
  currentOdometer,
  isDeleting,
  onDelete,
}: VehicleSummaryCardProps) {
  const vehicleName = `${vehicle.brand} ${vehicle.model}`.trim() || "Veículo";

  return (
    <section className="vehicle-details-card" aria-labelledby="vehicle-details-title">
      <div className="vehicle-details-main">
        <span className="vehicle-details-icon" aria-hidden>
          <CarIcon />
        </span>

        <div className="vehicle-details-copy">
          <h1 id="vehicle-details-title">{vehicleName}</h1>
          <div className="vehicle-details-meta">
            <span className="plate-badge">{vehicle.plate}</span>
            <span className="vehicle-meta-item">
              <CalendarIcon aria-hidden />
              {vehicle.manufactureYear}
            </span>
            {currentOdometer !== null ? (
              <span className="vehicle-meta-item">
                <GaugeIcon aria-hidden />
                {formatOdometer(currentOdometer)}
              </span>
            ) : null}
          </div>
        </div>
      </div>

      <div className="vehicle-details-actions" aria-label={`Ações para ${vehicleName}`}>
        <a className="vehicle-action-button" href={`/vehicles/${vehicle.id}/edit`}>
          <EditIcon aria-hidden />
          <span>Editar</span>
        </a>
        <button
          className="vehicle-action-button danger-action"
          type="button"
          disabled={isDeleting}
          onClick={onDelete}
        >
          <TrashIcon aria-hidden />
          <span>{isDeleting ? "Excluindo..." : "Excluir"}</span>
        </button>
      </div>
    </section>
  );
}

type HistorySharingCardProps = {
  sharing: VehicleHistorySharing | null;
  isUpdating: boolean;
  feedback: string;
  onEnable: () => void;
  onDisable: () => void;
  onCopy: () => void;
};

function HistorySharingCard({
  sharing,
  isUpdating,
  feedback,
  onEnable,
  onDisable,
  onCopy,
}: HistorySharingCardProps) {
  const publicPath = sharing?.publicId ? `/history/${sharing.publicId}` : "";

  return (
    <section className="history-sharing-card" aria-labelledby="history-sharing-title">
      <div>
        <p className="history-sharing-eyebrow">Histórico digital</p>
        <h2 id="history-sharing-title">Compartilhamento público</h2>
        <p>
          Gere um link sem dados pessoais para apresentar as manutenções registradas deste veículo.
        </p>
      </div>

      {sharing?.enabled && publicPath ? (
        <div className="history-sharing-actions">
          <a className="secondary-button" href={publicPath} target="_blank" rel="noreferrer">
            Visualizar histórico
          </a>
          <button className="secondary-button" type="button" onClick={onCopy}>
            Copiar link
          </button>
          <button className="text-button danger-text" type="button" disabled={isUpdating} onClick={onDisable}>
            {isUpdating ? "Desativando..." : "Desativar"}
          </button>
        </div>
      ) : (
        <button className="primary-button history-sharing-enable" type="button" disabled={isUpdating} onClick={onEnable}>
          {isUpdating ? "Ativando..." : "Ativar compartilhamento"}
        </button>
      )}

      {feedback ? <p className="history-sharing-feedback" role="status">{feedback}</p> : null}
    </section>
  );
}

export function VehicleDetailsPage() {
  const vehicleId = getVehicleIdFromPath(window.location.pathname);
  const [vehicle, setVehicle] = useState<Vehicle | null>(null);
  const [maintenances, setMaintenances] = useState<Maintenance[]>([]);
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isDeletingVehicle, setIsDeletingVehicle] = useState(false);
  const [deletingMaintenanceId, setDeletingMaintenanceId] = useState<string | null>(null);
  const [historySharing, setHistorySharing] = useState<VehicleHistorySharing | null>(null);
  const [isUpdatingSharing, setIsUpdatingSharing] = useState(false);
  const [sharingFeedback, setSharingFeedback] = useState("");
  const currentOdometer = useMemo(() => {
    if (maintenances.length === 0) {
      return null;
    }

    return Math.max(...maintenances.map((maintenance) => maintenance.odometer));
  }, [maintenances]);

  useEffect(() => {
    let isMounted = true;

    async function loadDetails() {
      if (!vehicleId) {
        setErrorMessage("Veículo não encontrado.");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setErrorMessage("");

      try {
        const [vehicleResponse, maintenancesResponse] = await Promise.all([
          getVehicle(vehicleId),
          listMaintenances(vehicleId),
        ]);

        if (isMounted) {
          setVehicle(vehicleResponse);
          setMaintenances(maintenancesResponse);
        }
      } catch (error) {
        if (isMounted) {
          setErrorMessage(getVehicleDetailsErrorMessage(error));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadDetails();

    return () => {
      isMounted = false;
    };
  }, [vehicleId]);

  useEffect(() => {
    let isMounted = true;

    async function loadSharing() {
      if (!vehicleId) return;

      try {
        const response = await getVehicleHistorySharing(vehicleId);
        if (isMounted) setHistorySharing(response);
      } catch {
        if (isMounted) setSharingFeedback("Não foi possível consultar o compartilhamento.");
      }
    }

    void loadSharing();

    return () => {
      isMounted = false;
    };
  }, [vehicleId]);

  async function handleDeleteVehicle() {
    if (!vehicle) {
      return;
    }

    const shouldDelete = window.confirm(`Excluir o veículo ${vehicle.plate}?`);

    if (!shouldDelete) {
      return;
    }

    setIsDeletingVehicle(true);
    setErrorMessage("");

    try {
      await deleteVehicle(vehicle.id);
      window.location.assign("/vehicles");
    } catch (error) {
      setErrorMessage(getVehicleDetailsErrorMessage(error) || "Não foi possível excluir o veículo.");
    } finally {
      setIsDeletingVehicle(false);
    }
  }

  async function handleDeleteMaintenance(maintenance: Maintenance) {
    const shouldDelete = window.confirm(`Excluir a manutenção "${maintenance.description}"?`);

    if (!shouldDelete) {
      return;
    }

    setDeletingMaintenanceId(maintenance.id);
    setErrorMessage("");

    try {
      await deleteMaintenance(vehicleId, maintenance.id);
      setMaintenances((currentMaintenances) =>
        currentMaintenances.filter((currentMaintenance) => currentMaintenance.id !== maintenance.id),
      );
    } catch (error) {
      setErrorMessage(getVehicleDetailsErrorMessage(error) || "Não foi possível excluir a manutenção.");
    } finally {
      setDeletingMaintenanceId(null);
    }
  }

  async function handleEnableSharing() {
    setIsUpdatingSharing(true);
    setSharingFeedback("");
    try {
      const response = await enableVehicleHistorySharing(vehicleId);
      setHistorySharing(response);
      setSharingFeedback("Compartilhamento ativado. O histórico pode levar alguns segundos para aparecer.");
    } catch {
      setSharingFeedback("Não foi possível ativar o compartilhamento.");
    } finally {
      setIsUpdatingSharing(false);
    }
  }

  async function handleDisableSharing() {
    setIsUpdatingSharing(true);
    setSharingFeedback("");
    try {
      await disableVehicleHistorySharing(vehicleId);
      setHistorySharing({ enabled: false, publicId: null });
      setSharingFeedback("Compartilhamento desativado e link anterior invalidado.");
    } catch {
      setSharingFeedback("Não foi possível desativar o compartilhamento.");
    } finally {
      setIsUpdatingSharing(false);
    }
  }

  async function handleCopySharingLink() {
    if (!historySharing?.publicId) return;
    const url = `${window.location.origin}/history/${historySharing.publicId}`;
    try {
      await navigator.clipboard.writeText(url);
      setSharingFeedback("Link copiado.");
    } catch {
      setSharingFeedback(url);
    }
  }

  return (
    <div className="vehicles-app">
      <AppSidebar />

      <main className="vehicles-content vehicle-details-page" aria-labelledby="vehicle-details-title">
      {isLoading ? (
        <section className="vehicles-status-card details-status-card" role="status" aria-live="polite">
          <span className="loading-spinner" aria-hidden />
          <p>Carregando detalhes do veículo...</p>
        </section>
      ) : null}

      {!isLoading && errorMessage && !vehicle ? (
        <section className="vehicles-status-card details-status-card error-status" role="alert">
          <h1 id="vehicle-details-title">Detalhes do veículo</h1>
          <p>{errorMessage}</p>
          <a className="secondary-button vehicle-status-action" href="/vehicles">
            Voltar para veículos
          </a>
        </section>
      ) : null}

      {!isLoading && vehicle ? (
        <>
          {errorMessage ? (
            <p className="form-error details-page-error" role="alert">
              {errorMessage}
            </p>
          ) : null}

          <div className="vehicle-details-toolbar">
            <a className="primary-button vehicles-header-button" href="/vehicles/new">
              <PlusIcon aria-hidden />
              <span>Adicionar Veículo</span>
            </a>
          </div>

          <VehicleSummaryCard
            vehicle={vehicle}
            currentOdometer={currentOdometer}
            isDeleting={isDeletingVehicle}
            onDelete={handleDeleteVehicle}
          />

          <HistorySharingCard
            sharing={historySharing}
            isUpdating={isUpdatingSharing}
            feedback={sharingFeedback}
            onEnable={handleEnableSharing}
            onDisable={handleDisableSharing}
            onCopy={handleCopySharingLink}
          />

          <MaintenanceHistory
            vehicleId={vehicle.id}
            maintenances={maintenances}
            deletingMaintenanceId={deletingMaintenanceId}
            onDeleteMaintenance={handleDeleteMaintenance}
          />
        </>
      ) : null}
      </main>
    </div>
  );
}
