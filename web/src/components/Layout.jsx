import React from 'react'
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const Icon = ({ d, size = 22 }) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
    strokeLinecap="round" strokeLinejoin="round" style={{ width: size, height: size }}>
    <path d={d} />
  </svg>
)

const nav = [
  { to: '/', label: 'Dashboard', icon: 'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z', exact: true },
]
const navMain = [
  { to: '/employees', label: 'Angajați', icon: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 7a4 4 0 100 8 4 4 0 000-8z' },
  { to: '/projects', label: 'Proiecte', icon: 'M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z' },
  { to: '/invoices', label: 'Facturi', icon: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M16 13H8M16 17H8M10 9H8' },
  { to: '/reports', label: 'Rapoarte', icon: 'M9 17v-2m3 2v-4m3 4v-6M4 6h16M4 10h16M4 14h8' },
  { to: '/payroll', label: 'Salarizare', icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
]
const navTools = [
  { to: '/leaves', label: 'Concedii', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
  { to: '/work-schedule', label: 'Program lucru', icon: 'M12 8v4l3 3M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
  { to: '/company', label: 'Firmă', icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4' },
]

const bottomNav = [
  { to: '/', label: 'Acasă', icon: 'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z', exact: true },
  { to: '/employees', label: 'Angajați', icon: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 7a4 4 0 100 8 4 4 0 000-8z' },
  { to: '/invoices', label: 'Facturi', icon: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6' },
  { to: '/reports', label: 'Rapoarte', icon: 'M9 17v-2m3 2v-4m3 4v-6M4 6h16' },
]

const pageLabels = {
  '/': 'Dashboard', '/employees': 'Angajați', '/projects': 'Proiecte',
  '/invoices': 'Facturi', '/reports': 'Rapoarte', '/payroll': 'Salarizare',
  '/company': 'Firmă', '/leaves': 'Concedii', '/work-schedule': 'Program lucru',
}

export default function Layout() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = React.useState(false)
  const [moreOpen, setMoreOpen] = React.useState(false)

  const handleLogout = () => { logout(); navigate('/login') }
  const linkClass = ({ isActive }) => 'nav-link' + (isActive ? ' active' : '')
  const closeNav = () => setSidebarOpen(false)

  const pageTitle = pageLabels[location.pathname] || 'Gestiune Firmă'

  return (
    <div className="app-shell">
      {/* Desktop sidebar */}
      {sidebarOpen && <div className="sidebar-overlay" onClick={() => setSidebarOpen(false)} />}
      <aside className={`sidebar${sidebarOpen ? ' sidebar-open' : ''}`}>
        <div className="sidebar-brand">
          <h2>Gestiune Firmă</h2>
          <span>webliftdigital</span>
        </div>
        <nav className="sidebar-nav">
          {nav.map(({ to, label, icon, exact }) => (
            <NavLink key={to} to={to} end={exact} className={linkClass} onClick={closeNav}>
              <Icon d={icon} />{label}
            </NavLink>
          ))}
          <div className="nav-section">Principal</div>
          {navMain.map(({ to, label, icon }) => (
            <NavLink key={to} to={to} className={linkClass} onClick={closeNav}>
              <Icon d={icon} />{label}
            </NavLink>
          ))}
          <div className="nav-section">Unelte</div>
          {navTools.map(({ to, label, icon }) => (
            <NavLink key={to} to={to} className={linkClass} onClick={closeNav}>
              <Icon d={icon} />{label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <button className="logout-btn" onClick={handleLogout}>
            <Icon d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            Deconectare
          </button>
        </div>
      </aside>

      <main className="main-content">
        {/* Mobile gradient header */}
        <div className="mobile-header">
          <button className="mobile-menu-btn" onClick={() => setSidebarOpen(o => !o)}>
            <Icon d="M4 6h16M4 12h16M4 18h16" size={20} />
          </button>
          <span className="mobile-header-title">{pageTitle}</span>
          <button className="mobile-logout-btn" onClick={handleLogout}>
            <Icon d="M17 16l4-4m0 0l-4-4m4 4H7" size={20} />
          </button>
        </div>

        <Outlet />

        {/* Mobile bottom navigation */}
        <nav className="bottom-nav">
          {bottomNav.map(({ to, label, icon, exact }) => (
            <NavLink key={to} to={to} end={exact} className={({ isActive }) => 'bottom-nav-item' + (isActive ? ' active' : '')}>
              <Icon d={icon} size={22} />
              <span>{label}</span>
            </NavLink>
          ))}
          <button className="bottom-nav-item" onClick={() => setMoreOpen(true)}>
            <Icon d="M4 6h16M4 12h16M4 18h7" size={22} />
            <span>Mai mult</span>
          </button>
        </nav>
      </main>

      {/* More menu sheet */}
      {moreOpen && (
        <div className="modal-overlay" onClick={() => setMoreOpen(false)}>
          <div className="more-sheet" onClick={e => e.stopPropagation()}>
            <div className="more-sheet-handle" />
            <h3>Meniu complet</h3>
            <div className="more-grid">
              {[...navMain, ...navTools].map(({ to, label, icon }) => (
                <button key={to} className="more-item" onClick={() => { navigate(to); setMoreOpen(false) }}>
                  <span className="more-icon"><Icon d={icon} size={20} /></span>
                  <span>{label}</span>
                </button>
              ))}
            </div>
            <button className="more-logout" onClick={handleLogout}>Deconectare</button>
          </div>
        </div>
      )}
    </div>
  )
}
