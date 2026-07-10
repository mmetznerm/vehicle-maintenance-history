import type { Maintenance } from "../types/maintenance";

export function formatMaintenanceDate(value: string) {
  const date = new Date(`${value}T00:00:00`);

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(date);
}

export function formatOdometer(value: number) {
  return `${new Intl.NumberFormat("pt-BR").format(value)} km`;
}

export function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
}

export function sortMaintenances(maintenances: Maintenance[]) {
  return [...maintenances].sort((left, right) => {
    const dateComparison = right.maintenanceDate.localeCompare(left.maintenanceDate);

    if (dateComparison !== 0) {
      return dateComparison;
    }

    return right.odometer - left.odometer;
  });
}
