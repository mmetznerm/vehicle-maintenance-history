import type { Maintenance } from "../types/maintenance";
import { EditIcon, PlusIcon, TrashIcon, WrenchIcon } from "./Icons";
import { formatCurrency, formatMaintenanceDate, formatOdometer, sortMaintenances } from "./maintenanceFormat";

type MaintenanceHistoryProps = {
  vehicleId: string;
  maintenances: Maintenance[];
  deletingMaintenanceId: string | null;
  onDeleteMaintenance: (maintenance: Maintenance) => void;
};

export function MaintenanceHistory({
  vehicleId,
  maintenances,
  deletingMaintenanceId,
  onDeleteMaintenance,
}: MaintenanceHistoryProps) {
  const sortedMaintenances = sortMaintenances(maintenances);

  if (sortedMaintenances.length === 0) {
    return (
      <section className="empty-maintenance-card" aria-labelledby="empty-maintenance-title">
        <span className="empty-vehicles-icon" aria-hidden>
          <WrenchIcon />
        </span>
        <h2 id="empty-maintenance-title">Nenhuma manutenção cadastrada</h2>
        <p>Cadastre a primeira manutenção para acompanhar custos, datas e quilometragem.</p>
        <a className="primary-button vehicle-add-button" href={`/vehicles/${vehicleId}/maintenances/new`}>
          <PlusIcon aria-hidden />
          <span>Adicionar manutenção</span>
        </a>
      </section>
    );
  }

  return (
    <section className="maintenance-section" aria-labelledby="maintenance-history-title">
      <header className="maintenance-header">
        <h2 id="maintenance-history-title">Histórico de manutenções</h2>
        <a className="primary-button maintenance-add-button" href={`/vehicles/${vehicleId}/maintenances/new`}>
          <PlusIcon aria-hidden />
          <span>Adicionar manutenção</span>
        </a>
      </header>

      <div className="maintenance-table-wrap">
        <table className="maintenance-table">
          <thead>
            <tr>
              <th>Data</th>
              <th>Odômetro</th>
              <th>Descrição</th>
              <th>Custo</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {sortedMaintenances.map((maintenance) => (
              <tr key={maintenance.id}>
                <td data-label="Data">{formatMaintenanceDate(maintenance.maintenanceDate)}</td>
                <td data-label="Odômetro">{formatOdometer(maintenance.odometer)}</td>
                <td data-label="Descrição">
                  <span className="maintenance-description">
                    <WrenchIcon aria-hidden />
                    {maintenance.description}
                  </span>
                </td>
                <td data-label="Custo">{formatCurrency(Number(maintenance.cost))}</td>
                <td data-label="Ações">
                  <div className="maintenance-row-actions">
                    <a
                      className="icon-action-button"
                      href={`/vehicles/${vehicleId}/maintenances/${maintenance.id}/edit`}
                      aria-label={`Editar manutenção ${maintenance.description}`}
                    >
                      <EditIcon aria-hidden />
                    </a>
                    <button
                      className="icon-action-button danger-icon-action"
                      type="button"
                      disabled={deletingMaintenanceId === maintenance.id}
                      aria-label={`Excluir manutenção ${maintenance.description}`}
                      onClick={() => onDeleteMaintenance(maintenance)}
                    >
                      <TrashIcon aria-hidden />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
