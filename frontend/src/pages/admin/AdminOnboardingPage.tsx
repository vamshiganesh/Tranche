import { useCallback, useEffect, useState } from 'react'
import {
  approveInvestorKyc,
  approveIssuerKyb,
  listPendingInvestors,
  listPendingIssuers,
  rejectInvestorKyc,
  rejectIssuerKyb,
  type PendingInvestor,
  type PendingIssuer,
} from '../../api/onboarding'
import { ApiClientError } from '../../api/client'
import { VerificationStatusBadge } from '../../components/VerificationCallout'

export function AdminOnboardingPage() {
  const [investors, setInvestors] = useState<PendingInvestor[]>([])
  const [issuers, setIssuers] = useState<PendingIssuer[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [inv, iss] = await Promise.all([listPendingInvestors(), listPendingIssuers()])
      setInvestors(inv)
      setIssuers(iss)
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function handleInvestor(userId: string, approve: boolean) {
    if (approve) await approveInvestorKyc(userId)
    else await rejectInvestorKyc(userId)
    await load()
  }

  async function handleIssuer(userId: string, approve: boolean) {
    if (approve) await approveIssuerKyb(userId)
    else await rejectIssuerKyb(userId)
    await load()
  }

  if (loading) {
    return (
      <div className="loading-center">
        <div className="spinner" />
      </div>
    )
  }

  return (
    <>
      <header className="page-header">
        <h2>Onboarding queue</h2>
        <p>Approve investor KYC and issuer company verification before they can transact.</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
        <div className="card-header">
          <h3>Investor KYC</h3>
          <span className="queue-count">{investors.length} pending</span>
        </div>
        {investors.length === 0 ? (
          <div className="empty-state">
            <h3>Queue is clear</h3>
            <p>No investors are awaiting identity verification.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Status</th>
                  <th>Registered</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {investors.map((row) => (
                  <tr key={row.userId}>
                    <td>
                      <strong>{row.fullName}</strong>
                    </td>
                    <td>{row.email}</td>
                    <td>
                      <VerificationStatusBadge status={row.kycStatus} />
                    </td>
                    <td>{new Date(row.registeredAt).toLocaleDateString()}</td>
                    <td>
                      <div className="table-actions">
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() => handleInvestor(row.userId, true)}
                        >
                          Approve
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => handleInvestor(row.userId, false)}
                        >
                          Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Issuer KYB</h3>
          <span className="queue-count">{issuers.length} pending</span>
        </div>
        {issuers.length === 0 ? (
          <div className="empty-state">
            <h3>Queue is clear</h3>
            <p>No issuers are awaiting company verification.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Company</th>
                  <th>Contact</th>
                  <th>Reg. number</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {issuers.map((row) => (
                  <tr key={row.userId}>
                    <td>
                      <strong>{row.companyName}</strong>
                    </td>
                    <td>
                      {row.fullName}
                      <div className="muted">{row.email}</div>
                    </td>
                    <td className="num">{row.registrationNumber ?? '—'}</td>
                    <td>
                      <VerificationStatusBadge status={row.verificationStatus} />
                    </td>
                    <td>
                      <div className="table-actions">
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() => handleIssuer(row.userId, true)}
                        >
                          Approve
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => handleIssuer(row.userId, false)}
                        >
                          Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  )
}
