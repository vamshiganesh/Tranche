import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listOpportunities } from '../../api/opportunities'
import type { OpportunitySummary } from '../../api/types'
import { useAuth } from '../../context/AuthContext'
import { StatusBadge } from '../../components/StatusBadge'
import { VerificationCallout } from '../../components/VerificationCallout'
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

  const verificationStatus = user?.issuerVerificationStatus
  const canCreate = verificationStatus === 'APPROVED'

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
      {verificationStatus && verificationStatus !== 'APPROVED' && (
        <VerificationCallout
          status={verificationStatus}
          title="Company verification"
          action={
            verificationStatus === 'REJECTED' ? (
              <Link to="/issuer/onboarding" className="btn btn-primary btn-sm">
                Update profile
              </Link>
            ) : undefined
          }
        >
          {verificationStatus === 'REJECTED'
            ? 'Your company profile was not approved. Update your details and resubmit for review before creating opportunities.'
            : 'An administrator is reviewing your company profile. You can create opportunities once approved.'}
        </VerificationCallout>
      )}

      <header className="page-header page-header-row">
        <div>
          <h2>My invoices</h2>
          <p>Create opportunities and submit them for platform review.</p>
        </div>
        {canCreate && (
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => navigate('/issuer/new')}
          >
            New opportunity
          </button>
        )}
      </header>

      <div className="card">
        {items.length === 0 ? (
          <div className="empty-state">
            <h3>No opportunities yet</h3>
            <p>
              {canCreate
                ? 'Create your first invoice opportunity to begin the review process.'
                : 'Complete company verification to start listing invoice opportunities.'}
            </p>
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
