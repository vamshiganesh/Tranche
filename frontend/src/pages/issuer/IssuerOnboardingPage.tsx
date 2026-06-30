import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createIssuerProfile, getIssuerProfile, resubmitIssuerProfile } from '../../api/issuers'
import { ApiClientError } from '../../api/client'
import { VerificationStatusBadge } from '../../components/VerificationCallout'
import { useAuth } from '../../context/AuthContext'

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
    <div className="issuer-onboarding-page">
      <header className="page-header page-header-row">
        <div>
          <h2>{isResubmit ? 'Update company profile' : 'Company profile'}</h2>
          <p>
            {isResubmit
              ? 'Correct your details and resubmit for administrator review.'
              : 'Tell us about your business before listing invoice opportunities.'}
          </p>
        </div>
        {isResubmit && <VerificationStatusBadge status="REJECTED" />}
      </header>

      <form className="card form-card issuer-onboarding-card" onSubmit={handleSubmit}>
        {isResubmit && (
          <div className="issuer-onboarding-notice">
            <h4>Previous submission not approved</h4>
            <p>
              Update your company information below. After resubmitting, your profile returns to
              the admin onboarding queue for review.
            </p>
          </div>
        )}

        <div className="card-body">
          {error && <div className="alert alert-error">{error}</div>}
          <div className="form-grid form-grid-single">
            <div className="field field-span-2">
              <label htmlFor="companyName">Company name</label>
              <input
                id="companyName"
                className="input"
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                placeholder="Acme Corp"
                required
              />
            </div>
            <div className="field field-span-2">
              <label htmlFor="registrationNumber">Registration number</label>
              <input
                id="registrationNumber"
                className="input"
                value={registrationNumber}
                onChange={(e) => setRegistrationNumber(e.target.value)}
                placeholder="Optional — company registry ID"
              />
            </div>
          </div>
        </div>

        <div className="card-footer">
          <Link to="/issuer" className="btn btn-secondary btn-sm">
            Cancel
          </Link>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : isResubmit ? 'Resubmit for review' : 'Submit for review'}
          </button>
        </div>
      </form>
    </div>
  )
}
