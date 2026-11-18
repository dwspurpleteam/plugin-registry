import { NavLink, Outlet } from "react-router-dom";

export function AppLayout() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/plugins" className="brand" aria-label="Plugin Registry — на главную">
          <span className="brand-mark" aria-hidden="true">
            PR
          </span>
          <span>
            <strong>Plugin</strong>
            <small>Registry</small>
          </span>
        </NavLink>
        <nav className="main-nav" aria-label="Основная навигация">
          <NavLink
            to="/plugins"
            className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
          >
            <span className="nav-icon" aria-hidden="true">◇</span>
            Каталог плагинов
          </NavLink>
        </nav>
        <div className="topbar-meta">
          <div>
            <span className="eyebrow">ITMO · Plugin platform</span>
          </div>
          <div className="environment-badge">Development</div>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
