import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { formatCurrencyPrecise } from '../lib/format'

const investorLinks = [
  { to: '/marketplace', label: 'Marketplace' },
  { to: '/portfolio', label: 'Portfolio' },
]

const issuerLinks = [
  { to: '/issuer', label: 'My invoices' },
  { to: '/issuer/new', label: 'New opportunity' },
]

const adminLinks = [
  { to: '/admin', label: 'Review queue' },
  { to: '/admin/opportunities', label: 'All opportunities' },
]

export function AppShell({ title }: { title: string }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  if (!user) return null

  const links =
    user.role === 'ADMIN' ? adminLinks : user.role === 'ISSUER' ? issuerLinks : investorLinks

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <h1>Tranche</h1>
          <p>Invoice discounting</p>
        </div>
        <nav className="sidebar-nav">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
              end={l.to !== '/admin/opportunities'}
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip">
            <strong>{user.fullName}</strong>
            <span>{user.role.toLowerCase()}</span>
          </div>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            style={{ marginTop: '0.75rem', color: '#a39b92' }}
            onClick={() => {
              logout()
              navigate('/login')
            }}
          >
            Sign out
          </button>
        </div>
      </aside>
      <div className="main-area">
        <header className="topbar">
          <span className="topbar-title">{title}</span>
          {user.role === 'INVESTOR' && user.walletBalance != null && (
            <span className="wallet-pill">
              Available {formatCurrencyPrecise(user.walletBalance)}
            </span>
          )}
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
