import { NavLink, Outlet } from "react-router-dom";

export function AppLayout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
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

        <div className="sidebar-note">
          <span className="status-dot" aria-hidden="true" />
          <div>
            <strong>Registry API</strong>
            <span>Spring service</span>
          </div>
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <div>
            <span className="eyebrow">ITMO · Plugin platform</span>
          </div>
          <div className="environment-badge">Development</div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
