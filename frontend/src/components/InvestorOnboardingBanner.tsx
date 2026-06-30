import { useState } from 'react'
import { applyDemoCredit } from '../api/investors'
import { ApiClientError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { formatCurrencyPrecise } from '../lib/format'

export function InvestorOnboardingBanner() {
  const { user, refreshUser } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!user || user.role !== 'INVESTOR') return null

  const needsKyc = user.kycStatus !== 'APPROVED'
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

  return (
    <div className="card onboarding-banner">
      <h3>Complete your setup</h3>
      {needsKyc && (
        <p>
          Identity verification is <strong>pending admin approval</strong>. You cannot commit
          funds until KYC is approved.
        </p>
      )}
      {needsFunds && (
        <p>
          Your wallet balance is {formatCurrencyPrecise(user.walletBalance ?? 0)}. Add demo
          funds to try the allocation flow locally.
        </p>
      )}
      {error && <div className="alert alert-error">{error}</div>}
      {needsFunds && (
        <button
          type="button"
          className="btn btn-primary btn-sm"
          disabled={loading}
          onClick={addDemoFunds}
        >
          {loading ? 'Adding funds…' : 'Add demo funds ($3M)'}
        </button>
      )}
    </div>
  )
}
