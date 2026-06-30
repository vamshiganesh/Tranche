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
    return <div className="loading-center"><span className="spinner" /></div>
  }

  return (
    <div className="stack-lg">
      <p className="subtitle">
        Approve investor KYC and issuer company verification before they can transact.
      </p>
      {error && <div className="alert alert-error">{error}</div>}

      <section className="card">
        <h3>Pending investor KYC</h3>
        {investors.length === 0 ? (
          <p className="muted">No investors awaiting review.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Registered</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {investors.map((row) => (
                <tr key={row.userId}>
                  <td>{row.fullName}</td>
                  <td>{row.email}</td>
                  <td>{new Date(row.registeredAt).toLocaleDateString()}</td>
                  <td className="table-actions">
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
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="card">
        <h3>Pending issuer KYB</h3>
        {issuers.length === 0 ? (
          <p className="muted">No issuers awaiting review.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Company</th>
                <th>Contact</th>
                <th>Reg. number</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {issuers.map((row) => (
                <tr key={row.userId}>
                  <td>{row.companyName}</td>
                  <td>
                    {row.fullName}
                    <br />
                    <span className="muted">{row.email}</span>
                  </td>
                  <td>{row.registrationNumber ?? '—'}</td>
                  <td className="table-actions">
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
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  )
}
