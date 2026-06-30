import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiClientError } from '../api/client'
import { useAuth } from '../context/AuthContext'

const DEMO_ACCOUNTS = [
  { label: 'Admin', email: 'admin@tranche.local' },
  { label: 'Issuer', email: 'issuer@tranche.local' },
  { label: 'Investor 1', email: 'investor1@tranche.local' },
  { label: 'Investor 2', email: 'investor2@tranche.local' },
]

const DEMO_PASSWORD = 'Password123!'

export function LoginPage() {
  const { user, login, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const locationState = (location.state as { email?: string; verified?: boolean } | null) ?? {}
  const [email, setEmail] = useState(locationState.email ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(locationState.verified ? 'Email verified. You can sign in.' : null)
  const [submitting, setSubmitting] = useState(false)

  if (!loading && user) {
    const dest =
      user.role === 'ADMIN' ? '/admin' : user.role === 'ISSUER' ? '/issuer' : '/marketplace'
    return <Navigate to={dest} replace />
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const me = await login(email, password)
      const dest =
        me.role === 'ADMIN' ? '/admin'
        : me.role === 'ISSUER' ? '/issuer'
        : '/marketplace'
      navigate(dest)
    } catch (err) {
      if (err instanceof ApiClientError) {
        if (err.code === 'EMAIL_NOT_VERIFIED') {
          navigate('/verify-email', { state: { email } })
          return
        }
        setError(err.message)
      } else {
        setError('Unable to sign in. Check that the API is running on port 8080.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  function fillDemo(accountEmail: string) {
    setEmail(accountEmail)
    setPassword(DEMO_PASSWORD)
  }

  return (
    <div className="login-page">
      <section className="login-hero">
        <div>
          <p style={{ marginBottom: 'var(--space-lg)' }}>
            <Link to="/" style={{ color: 'var(--ink-faint)', fontSize: '0.875rem' }}>
              ← Back to home
            </Link>
          </p>
          <h1>Fair allocation under pressure.</h1>
          <p className="lead">
            Tranche coordinates invoice commitments when demand exceeds supply. Every unit
            reserved atomically. Every transition audited.
          </p>
        </div>
        <dl className="stat-row">
          <div className="stat">
            <dt>Lock order</dt>
            <dd>Opportunity → Wallet</dd>
          </div>
          <div className="stat">
            <dt>Idempotency</dt>
            <dd>Per investor key</dd>
          </div>
        </dl>
      </section>
      <section className="login-panel">
        <div className="login-form-wrap">
          <h2>Sign in</h2>
          <p className="subtitle">Access your workspace with seeded demo credentials.</p>
          {success && <div className="alert alert-success">{success}</div>}
          {error && <div className="alert alert-error">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                className="input"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                className="input"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-ink btn-block" disabled={submitting}>
              {submitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
          <div className="demo-accounts">
            <p>Demo accounts</p>
            <div className="demo-chips">
              {DEMO_ACCOUNTS.map((a) => (
                <button
                  key={a.email}
                  type="button"
                  className="btn-chip"
                  onClick={() => fillDemo(a.email)}
                >
                  {a.label}
                </button>
              ))}
            </div>
          </div>
          <p style={{ marginTop: '1.25rem', fontSize: '0.875rem', color: 'var(--ink-muted)' }}>
            New here? <Link to="/register">Create an account</Link>
          </p>
        </div>
      </section>
    </div>
  )
}
