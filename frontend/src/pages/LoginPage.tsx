import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
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
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
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
      await login(email, password)
      navigate('/')
    } catch (err) {
      if (err instanceof ApiClientError) {
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
            <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
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
        </div>
      </section>
    </div>
  )
}
