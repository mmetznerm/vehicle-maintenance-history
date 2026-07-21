import { useEffect, useState } from "react";
import { formatOdometer } from "../components/maintenanceFormat";
import { ApiError, getPublicVehicleHistory } from "../services/api";
import type { PublicVehicleHistory } from "../types/vehicle";

function getPublicIdFromPath(pathname: string) {
  const match = pathname.match(/^\/history\/([^/]+)$/);
  return match?.[1] ? decodeURIComponent(match[1]) : "";
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", { timeZone: "UTC" }).format(
    new Date(`${value}T00:00:00Z`),
  );
}

export function PublicVehicleHistoryPage() {
  const publicId = getPublicIdFromPath(window.location.pathname);
  const [history, setHistory] = useState<PublicVehicleHistory | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    async function loadHistory() {
      try {
        const response = await getPublicVehicleHistory(publicId);
        if (mounted) setHistory(response);
      } catch (error) {
        if (!mounted) return;
        if (error instanceof ApiError && error.status === 404) {
          setErrorMessage("Este histórico não existe ou não está mais compartilhado.");
        } else {
          setErrorMessage("Não foi possível carregar o histórico do veículo agora.");
        }
      } finally {
        if (mounted) setIsLoading(false);
      }
    }

    if (publicId) {
      void loadHistory();
    } else {
      setErrorMessage("Link de histórico inválido.");
      setIsLoading(false);
    }

    return () => {
      mounted = false;
    };
  }, [publicId]);

  return (
    <main className="public-history-page">
      <header className="public-history-brand">
        <a href="/" aria-label="AutoLog">AutoLog</a>
        <span>Histórico digital do veículo</span>
      </header>

      {isLoading ? <section className="public-history-state" role="status">Carregando histórico...</section> : null}

      {!isLoading && errorMessage ? (
        <section className="public-history-state error-status" role="alert">
          <h1>Histórico indisponível</h1>
          <p>{errorMessage}</p>
        </section>
      ) : null}

      {!isLoading && history ? (
        <article className="public-history-content">
          <section className="public-history-vehicle">
            <p className="public-history-eyebrow">Veículo</p>
            <h1>{history.brand} {history.model}</h1>
            <p>{history.manufactureYear}{history.color ? ` • ${history.color}` : ""}</p>
          </section>

          <section aria-labelledby="public-maintenance-title">
            <div className="public-history-heading">
              <div>
                <p className="public-history-eyebrow">Linha do tempo</p>
                <h2 id="public-maintenance-title">Manutenções registradas</h2>
              </div>
              <span>{history.maintenances.length} registros</span>
            </div>

            {history.maintenances.length === 0 ? (
              <p className="public-history-empty">Nenhuma manutenção foi publicada até o momento.</p>
            ) : (
              <ol className="public-history-timeline">
                {history.maintenances.map((maintenance) => (
                  <li key={maintenance.id}>
                    <span className="public-history-marker" aria-hidden />
                    <div>
                      <time dateTime={maintenance.maintenanceDate}>{formatDate(maintenance.maintenanceDate)}</time>
                      <h3>{maintenance.description}</h3>
                      <p>{formatOdometer(maintenance.odometer)}</p>
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </section>

          <footer className="public-history-disclaimer">
            Este histórico foi informado pelo proprietário e não representa certificação oficial do veículo.
          </footer>
        </article>
      ) : null}
    </main>
  );
}
