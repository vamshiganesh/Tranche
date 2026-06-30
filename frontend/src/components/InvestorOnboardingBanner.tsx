import { useState } from 'react'
import { applyDemoCredit, resubmitKyc } from '../api/investors'
import { ApiClientError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { formatCurrencyPrecise } from '../lib/format'
import { VerificationCallout } from './VerificationCallout'

export function InvestorOnboardingBanner() {
  const { user, refreshUser } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!user || user.role !== 'INVESTOR') return null

  const kycStatus = user.kycStatus ?? 'PENDING'
  const needsKyc = kycStatus !== 'APPROVED'
  const needsFunds = (user.walletBalance ?? 0) <= 0

  if (!needsKyc && !needsFunds) return null

  async function addDemoFunds() {
    setLoading(true)
    setError(null)
    try {
      await applyDemoCredit()
      await refreshUser()
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }

  async function handleResubmitKyc() {
    setLoading(true)
    setError(null)
    try {
      await resubmitKyc()
      await refreshUser()
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="onboarding-stack">
      {needsKyc && (
        <VerificationCallout
          status={kycStatus}
          title="Identity verification"
          action={
            kycStatus === 'REJECTED' ? (
              <button
                type="button"
                className="btn btn-primary btn-sm"
                disabled={loading}
                onClick={handleResubmitKyc}
              >
                {loading ? 'Submitting…' : 'Resubmit for review'}
              </button>
            ) : undefined
          }
        >
          {kycStatus === 'REJECTED'
            ? 'Your identity verification was not approved. Resubmit to return to the admin review queue.'
            : 'An administrator must approve your identity before you can commit funds.'}
        </VerificationCallout>
      )}
      {needsFunds && (
        <div className="verification-callout verification-callout-info">
          <div className="verification-callout-head">
            <h4>Wallet funding</h4>
          </div>
          <p>
            Your available balance is {formatCurrencyPrecise(user.walletBalance ?? 0)}. Add demo
            funds to try the allocation flow locally.
          </p>
          {error && <div className="alert alert-error">{error}</div>}
          <div className="verification-callout-action">
            <button
              type="button"
              className="btn btn-primary btn-sm"
              disabled={loading}
              onClick={addDemoFunds}
            >
              {loading ? 'Adding funds…' : 'Add demo funds ($3M)'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
