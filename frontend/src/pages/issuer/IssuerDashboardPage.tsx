import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listOpportunities } from '../../api/opportunities'
import type { OpportunitySummary } from '../../api/types'
import { useAuth } from '../../context/AuthContext'
import { StatusBadge } from '../../components/StatusBadge'
import { formatCurrency, formatDate } from '../../lib/format'

function actionLabel(status: OpportunitySummary['status']): string {
  if (status === 'DRAFT') return 'Continue draft'
  return 'View details'
}

export function IssuerDashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [items, setItems] = useState<OpportunitySummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listOpportunities({ size: 100 })
      .then((res) => setItems(res.content))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="loading-center">
        <div className="spinner" />
      </div>
    )
  }

  return (
    <>
      {user?.issuerVerificationStatus === 'PENDING' && (
        <div className="card onboarding-banner" style={{ marginBottom: 'var(--space-lg)' }}>
          <h3>Company verification pending</h3>
          <p>
            An administrator must approve your company profile before you can create invoice
            opportunities.
          </p>
        </div>
      )}
      <header className="page-header">
        <h2>My invoices</h2>
        <p>Create opportunities and submit them for platform review.</p>
      </header>

      <div style={{ marginBottom: 'var(--space-lg)' }}>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => navigate('/issuer/new')}
        >
          New opportunity
        </button>
      </div>

      <div className="card">
        {items.length === 0 ? (
          <div className="empty-state">
            <h3>No opportunities yet</h3>
            <p>Create your first invoice opportunity to begin the review process.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Status</th>
                  <th>Face value</th>
                  <th>Units left</th>
                  <th>Maturity</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {items.map((o) => (
                  <tr key={o.id}>
                    <td>
                      <strong>{o.title}</strong>
                    </td>
                    <td>
                      <StatusBadge status={o.status} />
                    </td>
                    <td className="num">{formatCurrency(o.faceValue)}</td>
                    <td className="num">{o.remainingUnits}</td>
                    <td>{formatDate(o.maturityDate)}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => navigate(`/issuer/${o.id}`)}
                      >
                        {actionLabel(o.status)}
                      </button>
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
