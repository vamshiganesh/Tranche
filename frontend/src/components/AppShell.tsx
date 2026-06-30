import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { applyDemoCredit } from '../api/investors'
import { ApiClientError } from '../api/client'
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
  { to: '/admin/onboarding', label: 'Onboarding' },
  { to: '/admin/opportunities', label: 'All opportunities' },
]

export function AppShell({ title }: { title: string }) {
  const { user, logout, refreshUser } = useAuth()
  const navigate = useNavigate()
  const [crediting, setCrediting] = useState(false)

  if (!user) return null

  const links =
    user.role === 'ADMIN' ? adminLinks : user.role === 'ISSUER' ? issuerLinks : investorLinks

  const showDemoFunds =
    user.role === 'INVESTOR' && (user.walletBalance ?? 0) <= 0

  async function handleDemoCredit() {
    setCrediting(true)
    try {
      await applyDemoCredit()
      await refreshUser()
    } catch (err) {
      if (err instanceof ApiClientError) {
        alert(err.message)
      }
    } finally {
      setCrediting(false)
    }
  }

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
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {showDemoFunds && (
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={crediting}
                onClick={handleDemoCredit}
              >
                {crediting ? 'Adding…' : 'Add demo funds'}
              </button>
            )}
            {user.role === 'INVESTOR' && user.walletBalance != null && (
              <span className="wallet-pill">
                Available {formatCurrencyPrecise(user.walletBalance)}
              </span>
            )}
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
