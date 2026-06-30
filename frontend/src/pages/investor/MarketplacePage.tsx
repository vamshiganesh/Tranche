import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listOpportunities } from '../../api/opportunities'
import type { OpportunitySummary } from '../../api/types'
import { InvestorOnboardingBanner } from '../../components/InvestorOnboardingBanner'
import { RiskBadge, StatusBadge } from '../../components/StatusBadge'
import { formatCurrency, formatDate, formatPercent } from '../../lib/format'

function estimatedTotalUnits(o: OpportunitySummary): number {
  if (o.unitPrice > 0) {
    return Math.max(1, Math.round(o.faceValue / o.unitPrice))
  }
  return Math.max(o.remainingUnits, 1)
}

export function MarketplacePage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<OpportunitySummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listOpportunities({ status: 'LIVE', size: 50 })
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
        <h2>Marketplace</h2>
        <p>Open opportunities accepting commitments right now.</p>
      </header>

      {items.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <h3>No live opportunities</h3>
            <p>Check back after an admin publishes an approved invoice.</p>
          </div>
        </div>
      ) : (
        <div className="card">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Invoice</th>
                  <th>Grade</th>
                  <th>Face value</th>
                  <th>Discount</th>
                  <th>Unit price</th>
                  <th>Availability</th>
                  <th>Maturity</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {items.map((o) => {
                  const total = estimatedTotalUnits(o)
                  const subscribedPct = Math.round(
                    ((total - o.remainingUnits) / total) * 100
                  )

                  return (
                    <tr
                      key={o.id}
                      className="clickable"
                      onClick={() => navigate(`/marketplace/${o.id}`)}
                    >
                      <td>
                        <strong>{o.title}</strong>
                        <div style={{ marginTop: 4 }}>
                          <StatusBadge status={o.status} />
                        </div>
                      </td>
                      <td>
                        <RiskBadge grade={o.riskGrade} />
                      </td>
                      <td className="num">{formatCurrency(o.faceValue)}</td>
                      <td className="num">{formatPercent(o.discountRate)}</td>
                      <td className="num">{formatCurrency(o.unitPrice)}</td>
                      <td>
                        <div className="num">{o.remainingUnits} units left</div>
                        <div className="progress-bar" style={{ marginTop: 6, width: 120 }}>
                          <div
                            className="progress-bar-fill"
                            style={{ width: `${subscribedPct}%` }}
                          />
                        </div>
                      </td>
                      <td>{formatDate(o.maturityDate)}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={(e) => {
                            e.stopPropagation()
                            navigate(`/marketplace/${o.id}`)
                          }}
                        >
                          Invest
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  )
}
