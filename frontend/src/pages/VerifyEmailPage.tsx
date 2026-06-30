import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { resendVerification, verifyEmail } from '../api/auth'
import { ApiClientError } from '../api/client'

interface LocationState {
  email?: string
  devVerificationCode?: string
}

export function VerifyEmailPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state as LocationState | null) ?? {}

  const [email, setEmail] = useState(state.email ?? '')
  const [code, setCode] = useState(state.devVerificationCode ?? '')
  const [devHint, setDevHint] = useState(state.devVerificationCode ?? '')
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleVerify(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setMessage(null)
    setSubmitting(true)
    try {
      await verifyEmail(email, code)
      navigate('/login', {
        state: { email, verified: true },
      })
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(err.message)
      } else {
        setError('Verification failed.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleResend() {
    setError(null)
    setMessage(null)
    try {
      const res = await resendVerification(email)
      if (res.devVerificationCode) {
        setDevHint(res.devVerificationCode)
        setCode(res.devVerificationCode)
        setMessage('New code generated (dev mode — shown below).')
      } else {
        setMessage('If the account exists, a new code was sent.')
      }
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(err.message)
      }
    }
  }

  return (
    <div className="login-page">
      <section className="login-hero">
        <div>
          <h1>Verify your email.</h1>
          <p className="lead">
            Enter the six-digit code we sent to your inbox. In development, the code is also
            logged on the API server and may appear below.
          </p>
        </div>
      </section>
      <section className="login-panel">
        <div className="login-form-wrap">
          <h2>Email verification</h2>
          {error && <div className="alert alert-error">{error}</div>}
          {message && <div className="alert alert-success">{message}</div>}
          {devHint && (
            <div className="alert alert-info" style={{ fontFamily: 'var(--font-mono)' }}>
              Dev code: <strong>{devHint}</strong>
            </div>
          )}
          <form onSubmit={handleVerify}>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                className="input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="code">Verification code</label>
              <input
                id="code"
                className="input"
                inputMode="numeric"
                pattern="\d{6}"
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                required
              />
            </div>
            <button type="submit" className="btn btn-ink btn-block" disabled={submitting}>
              {submitting ? 'Verifying…' : 'Verify email'}
            </button>
          </form>
          <div style={{ marginTop: '1rem', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
            <button type="button" className="btn btn-secondary btn-sm" onClick={handleResend}>
              Resend code
            </button>
            <Link to="/login" className="btn btn-ghost btn-sm">
              Back to sign in
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
