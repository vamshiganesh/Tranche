import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createIssuerProfile, getIssuerProfile, resubmitIssuerProfile } from '../../api/issuers'
import { ApiClientError } from '../../api/client'
import { useAuth } from '../../context/AuthContext'
import { VerificationCallout } from '../../components/VerificationCallout'

export function IssuerOnboardingPage() {
  const { user, refreshUser } = useAuth()
  const navigate = useNavigate()
  const [companyName, setCompanyName] = useState('')
  const [registrationNumber, setRegistrationNumber] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [loadingProfile, setLoadingProfile] = useState(false)

  const isResubmit = user?.hasIssuerProfile && user.issuerVerificationStatus === 'REJECTED'

  useEffect(() => {
    if (!isResubmit) return
    setLoadingProfile(true)
    getIssuerProfile()
      .then((profile) => {
        setCompanyName(profile.companyName)
        setRegistrationNumber(profile.registrationNumber ?? '')
      })
      .catch(() => setError('Unable to load your company profile.'))
      .finally(() => setLoadingProfile(false))
  }, [isResubmit])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      if (isResubmit) {
        await resubmitIssuerProfile(companyName, registrationNumber || undefined)
      } else {
        await createIssuerProfile(companyName, registrationNumber || undefined)
      }
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

  if (loadingProfile) {
    return (
      <div className="loading-center">
        <div className="spinner" />
      </div>
    )
  }

  return (
    <>
      <header className="page-header">
        <h2>{isResubmit ? 'Update company profile' : 'Company profile'}</h2>
        <p>
          {isResubmit
            ? 'Correct the details below and resubmit for administrator review.'
            : 'Tell us about your business before listing invoice opportunities.'}
        </p>
      </header>

      {isResubmit && (
        <VerificationCallout status="REJECTED" title="Verification not approved">
          Your company profile was rejected. Update your details and resubmit — you will reappear
          in the admin onboarding queue.
        </VerificationCallout>
      )}

      <div className="card form-card">
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
              {submitting ? 'Saving…' : isResubmit ? 'Resubmit for review' : 'Submit for review'}
            </button>
          </div>
        </form>
      </div>
    </>
  )
}
