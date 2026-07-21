export type MaintenanceInconsistencyRule =
  | "ODOMETER_ROLLBACK"
  | "POSSIBLE_DUPLICATE"
  | "DATE_BEFORE_MANUFACTURE";

export type MaintenanceInconsistencySeverity = "WARNING" | "CRITICAL";
export type MaintenanceInconsistencyStatus = "ACTIVE" | "RESOLVED";

export type MaintenanceInconsistency = {
  alertId: string;
  rule: MaintenanceInconsistencyRule;
  severity: MaintenanceInconsistencySeverity;
  maintenanceIds: string[];
  summary: string;
  details: string;
  status: MaintenanceInconsistencyStatus;
  detectedAt: string;
  resolvedAt: string | null;
};
