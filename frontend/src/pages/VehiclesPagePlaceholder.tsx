import { clearAuthTokens } from "../services/authStorage";

export function VehiclesPagePlaceholder() {
  function handleLogout() {
    clearAuthTokens();
    window.location.assign("/login");
  }

  return (
    <main className="vehicles-placeholder-page">
      <section className="vehicles-placeholder">
        <p className="placeholder-eyebrow">AutoLog</p>
        <h1>Veículos</h1>
        <p>Login realizado com sucesso. Esta rota está pronta para receber a próxima tela.</p>
        <button type="button" className="secondary-button" onClick={handleLogout}>
          Sair
        </button>
      </section>
    </main>
  );
}
