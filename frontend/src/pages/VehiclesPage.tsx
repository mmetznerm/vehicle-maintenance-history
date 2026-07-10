import { useEffect, useState } from "react";
import { MaintenanceHistory } from "../components/MaintenanceHistory";
import {
  CalendarIcon,
  CarIcon,
  EditIcon,
  ExternalLinkIcon,
  PaletteIcon,
  PlusIcon,
  SettingsIcon,
  TrashIcon,
} from "../components/Icons";
import { ApiError, deleteMaintenance, deleteVehicle, listMaintenances, listVehicles } from "../services/api";
import { clearAuthTokens, getCurrentUserDisplayName } from "../services/authStorage";
import type { Maintenance } from "../types/maintenance";
import type { VehicleSummary } from "../types/vehicle";

function getVehiclesErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return "";
    }

    if (error.status >= 500) {
      return "Não foi possível conectar ao servidor. Verifique se a API está em execução.";
    }

    return error.message;
  }

  return "Não foi possível carregar seus veículos agora.";
}

function VehicleSidebar() {
  const userDisplayName = getCurrentUserDisplayName();

  function handleLogout() {
    clearAuthTokens();
    window.location.assign("/login");
  }

  return (
    <aside className="app-sidebar" aria-label="Navegação principal">
      <a className="sidebar-brand" href="/vehicles">
        AutoLog
      </a>

      <div className="sidebar-user">
        <div>
          <strong>Bem-vindo</strong>
          <span>{userDisplayName}</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <a className="sidebar-link is-active" href="/vehicles" aria-current="page">
          <CarIcon aria-hidden />
          <span>Painel</span>
        </a>
        <a className="sidebar-link" href="/vehicles/new">
          <PlusIcon aria-hidden />
          <span>Adicionar veículo</span>
        </a>
        <a className="sidebar-link" href="/settings">
          <SettingsIcon aria-hidden />
          <span>Configurações</span>
        </a>
      </nav>

      <button className="sidebar-logout" type="button" onClick={handleLogout}>
        Sair
      </button>
    </aside>
  );
}

function EmptyVehiclesState() {
  return (
    <section className="empty-vehicles-card" aria-labelledby="empty-vehicles-title">
      <span className="empty-vehicles-icon" aria-hidden>
        <CarIcon />
      </span>
      <h1 id="empty-vehicles-title">Nenhum veículo cadastrado</h1>
      <p>
        Você ainda não possui veículos em sua frota. Comece adicionando o seu primeiro veículo
        para acompanhar manutenções e relatórios.
      </p>
      <a className="primary-button vehicle-add-button" href="/vehicles/new">
        <PlusIcon aria-hidden />
        <span>Adicionar Veículo</span>
      </a>
    </section>
  );
}

type VehicleCardProps = {
  vehicle: VehicleSummary;
  isDeleting: boolean;
  showDetailsAction?: boolean;
  onDelete: (vehicle: VehicleSummary) => void;
};

function VehicleCard({ vehicle, isDeleting, showDetailsAction = true, onDelete }: VehicleCardProps) {
  const vehicleName = `${vehicle.brand} ${vehicle.model}`.trim();

  return (
    <article className="vehicle-card" aria-labelledby={`vehicle-${vehicle.id}-title`}>
      <div className="vehicle-card-main">
        <span className="vehicle-icon-badge" aria-hidden>
          <CarIcon />
        </span>

        <div className="vehicle-card-content">
          <h2 id={`vehicle-${vehicle.id}-title`}>{vehicleName || "Veículo sem modelo"}</h2>
          <div className="vehicle-meta-row">
            <span className="plate-badge">{vehicle.plate}</span>
            <span className="vehicle-meta-item">
              <CalendarIcon aria-hidden />
              {vehicle.manufactureYear}
            </span>
            <span className="vehicle-meta-item">
              <PaletteIcon aria-hidden />
              {vehicle.color}
            </span>
          </div>
        </div>
      </div>

      <div className="vehicle-actions" aria-label={`Ações para ${vehicleName || vehicle.plate}`}>
        {showDetailsAction ? (
          <a className="vehicle-action-button" href={`/vehicles/${vehicle.id}`}>
            <ExternalLinkIcon aria-hidden />
            <span>Detalhes</span>
          </a>
        ) : null}
        <a className="vehicle-action-button" href={`/vehicles/${vehicle.id}/edit`}>
          <EditIcon aria-hidden />
          <span>Editar</span>
        </a>
        <button
          className="vehicle-action-button danger-action"
          type="button"
          disabled={isDeleting}
          onClick={() => onDelete(vehicle)}
        >
          <TrashIcon aria-hidden />
          <span>{isDeleting ? "Excluindo..." : "Excluir"}</span>
        </button>
      </div>
    </article>
  );
}

export function VehiclesPage() {
  const [vehicles, setVehicles] = useState<VehicleSummary[]>([]);
  const [singleVehicleMaintenances, setSingleVehicleMaintenances] = useState<Maintenance[]>([]);
  const [errorMessage, setErrorMessage] = useState("");
  const [maintenanceErrorMessage, setMaintenanceErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [deletingVehicleId, setDeletingVehicleId] = useState<string | null>(null);
  const [deletingMaintenanceId, setDeletingMaintenanceId] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadVehicles() {
      setIsLoading(true);
      setErrorMessage("");
      setMaintenanceErrorMessage("");
      setSingleVehicleMaintenances([]);

      try {
        const vehiclesResponse = await listVehicles();

        if (vehiclesResponse.length === 1) {
          window.location.replace(`/vehicles/${vehiclesResponse[0].id}`);
          return;
        }

        if (isMounted) {
          setVehicles(vehiclesResponse);
        }

        if (vehiclesResponse.length === 1) {
          try {
            const maintenancesResponse = await listMaintenances(vehiclesResponse[0].id);

            if (isMounted) {
              setSingleVehicleMaintenances(maintenancesResponse);
            }
          } catch (error) {
            if (isMounted) {
              setMaintenanceErrorMessage(
                getVehiclesErrorMessage(error) || "Não foi possível carregar as manutenções.",
              );
            }
          }
        }
      } catch (error) {
        if (isMounted) {
          setErrorMessage(getVehiclesErrorMessage(error));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadVehicles();

    return () => {
      isMounted = false;
    };
  }, []);

  async function handleDeleteVehicle(vehicle: VehicleSummary) {
    const shouldDelete = window.confirm(`Excluir o veículo ${vehicle.plate}?`);

    if (!shouldDelete) {
      return;
    }

    setDeletingVehicleId(vehicle.id);
    setErrorMessage("");

    try {
      await deleteVehicle(vehicle.id);
      setVehicles((currentVehicles) =>
        currentVehicles.filter((currentVehicle) => currentVehicle.id !== vehicle.id),
      );
    } catch (error) {
      setErrorMessage(getVehiclesErrorMessage(error) || "Não foi possível excluir o veículo.");
    } finally {
      setDeletingVehicleId(null);
    }
  }

  async function handleDeleteMaintenance(maintenance: Maintenance) {
    const vehicle = vehicles[0];

    if (!vehicle) {
      return;
    }

    const shouldDelete = window.confirm(`Excluir a manutenção "${maintenance.description}"?`);

    if (!shouldDelete) {
      return;
    }

    setDeletingMaintenanceId(maintenance.id);
    setMaintenanceErrorMessage("");

    try {
      await deleteMaintenance(vehicle.id, maintenance.id);
      setSingleVehicleMaintenances((currentMaintenances) =>
        currentMaintenances.filter((currentMaintenance) => currentMaintenance.id !== maintenance.id),
      );
    } catch (error) {
      setMaintenanceErrorMessage(
        getVehiclesErrorMessage(error) || "Não foi possível excluir a manutenção.",
      );
    } finally {
      setDeletingMaintenanceId(null);
    }
  }

  return (
    <div className="vehicles-app">
      <VehicleSidebar />

      <main className="vehicles-content">
        {isLoading ? (
          <section className="vehicles-status-card" role="status" aria-live="polite">
            <span className="loading-spinner" aria-hidden />
            <p>Carregando veículos...</p>
          </section>
        ) : null}

        {!isLoading && errorMessage ? (
          <section className="vehicles-status-card error-status" role="alert">
            <h1 id="vehicles-page-title">Veículos</h1>
            <p>{errorMessage}</p>
            <button
              type="button"
              className="secondary-button"
              onClick={() => window.location.reload()}
            >
              Tentar novamente
            </button>
          </section>
        ) : null}

        {!isLoading && !errorMessage && vehicles.length === 0 ? <EmptyVehiclesState /> : null}

        {!isLoading && !errorMessage && vehicles.length > 0 ? (
          <section className="vehicles-list-section">
            <header className="vehicles-page-header">
              <div>
                <p className="section-eyebrow">AutoLog</p>
                <h1 id="vehicles-page-title">Veículos</h1>
                <p>
                  {vehicles.length} veículo{vehicles.length === 1 ? "" : "s"} cadastrado
                  {vehicles.length === 1 ? "" : "s"}.
                </p>
              </div>
            </header>

            <div className="vehicles-list" aria-label="Lista de veículos cadastrados">
              {vehicles.map((vehicle) => (
                <VehicleCard
                  key={vehicle.id}
                  vehicle={vehicle}
                  isDeleting={deletingVehicleId === vehicle.id}
                  showDetailsAction={vehicles.length > 1}
                  onDelete={handleDeleteVehicle}
                />
              ))}
            </div>

            {vehicles.length === 1 ? (
              <div className="single-vehicle-maintenance-panel">
                {maintenanceErrorMessage ? (
                  <p className="form-error" role="alert">
                    {maintenanceErrorMessage}
                  </p>
                ) : null}
                <MaintenanceHistory
                  vehicleId={vehicles[0].id}
                  maintenances={singleVehicleMaintenances}
                  deletingMaintenanceId={deletingMaintenanceId}
                  onDeleteMaintenance={handleDeleteMaintenance}
                />
              </div>
            ) : null}
          </section>
        ) : null}
      </main>
    </div>
  );
}
