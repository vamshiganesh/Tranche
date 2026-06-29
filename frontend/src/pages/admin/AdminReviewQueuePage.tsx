import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listOpportunities } from '../../api/opportunities'
import type { OpportunitySummary } from '../../api/types'
import { StatusBadge } from '../../components/StatusBadge'
import { formatCurrency, formatDate } from '../../lib/format'

export function AdminReviewQueuePage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<OpportunitySummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listOpportunities({ status: 'UNDER_REVIEW', size: 50 })
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
      <header className="page-header">
        <h2>Review queue</h2>
        <p>Issuer submissions awaiting approval before publication.</p>
      </header>

      <div className="card">
        {items.length === 0 ? (
          <div className="empty-state">
            <h3>Queue is clear</h3>
            <p>No opportunities are waiting for review.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Invoice</th>
                  <th>Face value</th>
                  <th>Discount</th>
                  <th>Risk</th>
                  <th>Maturity</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {items.map((o) => (
                  <tr
                    key={o.id}
                    className="clickable"
                    onClick={() => navigate(`/admin/opportunities/${o.id}`)}
                  >
                    <td>
                      <strong>{o.title}</strong>
                      <div style={{ marginTop: 4 }}>
                        <StatusBadge status={o.status} />
                      </div>
                    </td>
                    <td className="num">{formatCurrency(o.faceValue)}</td>
                    <td className="num">{o.discountRate}%</td>
                    <td>Grade {o.riskGrade}</td>
                    <td>{formatDate(o.maturityDate)}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={(e) => {
                          e.stopPropagation()
                          navigate(`/admin/opportunities/${o.id}`)
                        }}
                      >
                        Review
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
