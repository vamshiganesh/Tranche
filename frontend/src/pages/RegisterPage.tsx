import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/auth'
import { ApiClientError } from '../api/client'
import { PasswordRequirements } from '../components/PasswordRequirements'
import { RoleSelect } from '../components/RoleSelect'
import { isPasswordValid } from '../lib/password'
import type { Role } from '../api/types'

export function RegisterPage() {
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [role, setRole] = useState<Role>('INVESTOR')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [passwordTouched, setPasswordTouched] = useState(false)

  const passwordsMatch = password.length > 0 && password === confirmPassword
  const passwordValid = isPasswordValid(password)
  const confirmMismatch = confirmPassword.length > 0 && password !== confirmPassword

  const canSubmit = useMemo(
    () => passwordValid && passwordsMatch && fullName.trim() && email.trim(),
    [passwordValid, passwordsMatch, fullName, email]
  )

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setPasswordTouched(true)
    setError(null)

    if (!passwordValid) {
      setError('Password does not meet the required complexity rules.')
      return
    }
    if (!passwordsMatch) {
      setError('Passwords do not match.')
      return
    }

    setSubmitting(true)
    try {
      const res = await register({ email, password, fullName, role })
      navigate('/verify-email', {
        state: {
          email: res.email,
          devVerificationCode: res.devVerificationCode ?? undefined,
        },
      })
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(err.message)
      } else {
        setError('Unable to create account. Check that the API is running.')
      }
    } finally {
      setSubmitting(false)
    }
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
          <h1>Join Tranche.</h1>
          <p className="lead">
            Investors access live invoice opportunities. Issuers list receivables for
            review. Every account is verified before transacting.
          </p>
        </div>
      </section>
      <section className="login-panel">
        <div className="login-form-wrap">
          <h2>Create account</h2>
          <p className="subtitle">Choose your role, then verify your email to sign in.</p>
          {error && <div className="alert alert-error">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="fullName">Full name</label>
              <input
                id="fullName"
                className="input"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
            </div>
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
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onBlur={() => setPasswordTouched(true)}
                required
              />
              <PasswordRequirements password={password} show={passwordTouched || password.length > 0} />
            </div>
            <div className="field">
              <label htmlFor="confirmPassword">Confirm password</label>
              <input
                id="confirmPassword"
                className={`input${confirmMismatch ? ' input-invalid' : ''}`}
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
              {confirmMismatch && (
                <p className="field-hint field-hint-error">Passwords do not match.</p>
              )}
              {passwordsMatch && confirmPassword.length > 0 && (
                <p className="field-hint field-hint-success">Passwords match.</p>
              )}
            </div>
            <div className="field">
              <label id="role-label">I am a</label>
              <RoleSelect value={role} onChange={setRole} />
            </div>
            <button
              type="submit"
              className="btn btn-ink btn-block"
              disabled={submitting || !canSubmit}
            >
              {submitting ? 'Creating account…' : 'Create account'}
            </button>
          </form>
          <p style={{ marginTop: '1.25rem', fontSize: '0.875rem', color: 'var(--ink-muted)' }}>
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
        </div>
      </section>
    </div>
  )
}
