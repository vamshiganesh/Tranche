import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listOpportunities } from '../../api/opportunities'
import type { OpportunityStatus, OpportunitySummary } from '../../api/types'
import { StatusBadge } from '../../components/StatusBadge'
import { formatCurrency, formatDate } from '../../lib/format'

const STATUS_FILTERS: { label: string; value: OpportunityStatus | '' }[] = [
  { label: 'All', value: '' },
  { label: 'Live', value: 'LIVE' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'In review', value: 'UNDER_REVIEW' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Settled', value: 'SETTLED' },
]

export function AdminOpportunitiesPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<OpportunitySummary[]>([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<OpportunityStatus | ''>('')

  useEffect(() => {
    setLoading(true)
    listOpportunities({
      status: statusFilter || undefined,
      size: 100,
    })
      .then((res) => setItems(res.content))
      .finally(() => setLoading(false))
  }, [statusFilter])

  return (
    <>
      <header className="page-header">
        <h2>All opportunities</h2>
        <p>Full lifecycle view across issuers on the platform.</p>
      </header>

      <div className="filter-bar">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.label}
            type="button"
            className={`filter-chip${statusFilter === f.value ? ' active' : ''}`}
            onClick={() => setStatusFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>

      <div className="card">
        {loading ? (
          <div className="loading-center" style={{ minHeight: 160 }}>
            <div className="spinner" />
          </div>
        ) : items.length === 0 ? (
          <div className="empty-state">
            <h3>No matches</h3>
            <p>Try a different status filter.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
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
                  <tr
                    key={o.id}
                    className="clickable"
                    onClick={() => navigate(`/admin/opportunities/${o.id}`)}
                  >
                    <td className="num">#{o.id}</td>
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
                        onClick={(e) => {
                          e.stopPropagation()
                          navigate(`/admin/opportunities/${o.id}`)
                        }}
                      >
                        Open
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
