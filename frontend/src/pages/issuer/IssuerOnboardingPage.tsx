import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createIssuerProfile } from '../../api/issuers'
import { ApiClientError } from '../../api/client'
import { useAuth } from '../../context/AuthContext'

export function IssuerOnboardingPage() {
  const { refreshUser } = useAuth()
  const navigate = useNavigate()
  const [companyName, setCompanyName] = useState('')
  const [registrationNumber, setRegistrationNumber] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await createIssuerProfile(companyName, registrationNumber || undefined)
      await refreshUser()
      navigate('/issuer')
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(err.message)
      } else {
        setError('Unable to save company profile.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page-narrow">
      <div className="card form-card">
        <h2>Company profile</h2>
        <p className="subtitle" style={{ marginBottom: '1.5rem' }}>
          Tell us about your business. An administrator will review your company before you
          can list invoice opportunities.
        </p>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={handleSubmit} className="form-grid">
          <div className="field" style={{ gridColumn: '1 / -1' }}>
            <label htmlFor="companyName">Company name</label>
            <input
              id="companyName"
              className="input"
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              required
            />
          </div>
          <div className="field" style={{ gridColumn: '1 / -1' }}>
            <label htmlFor="registrationNumber">Registration number (optional)</label>
            <input
              id="registrationNumber"
              className="input"
              value={registrationNumber}
              onChange={(e) => setRegistrationNumber(e.target.value)}
            />
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Saving…' : 'Submit for review'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
