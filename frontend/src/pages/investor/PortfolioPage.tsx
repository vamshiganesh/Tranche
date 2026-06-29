import { useEffect, useState } from 'react'
import { fetchPortfolio } from '../../api/portfolio'
import type { PortfolioResponse } from '../../api/types'
import { PositionStatusBadge } from '../../components/StatusBadge'
import {
  formatCurrencyPrecise,
  formatDate,
  formatPercent,
  positionProfit,
} from '../../lib/format'

export function PortfolioPage() {
  const [data, setData] = useState<PortfolioResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchPortfolio()
      .then(setData)
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="loading-center">
        <div className="spinner" />
      </div>
    )
  }

  if (!data) return null

  const { summary, positions } = data
  const totalProfit = positionProfit(summary.totalInvested, summary.totalExpectedReturn)
  const showPricingNote =
    totalProfit === 0 &&
    positions.some((p) => positionProfit(p.investedAmount, p.expectedReturn) === 0)

  return (
    <>
      <header className="page-header">
        <h2>Portfolio</h2>
        <p>Your allocated positions across live and settled opportunities.</p>
      </header>

      <div className="metrics-row">
        <div className="metric-card">
          <div className="label">Total invested</div>
          <div className="value">{formatCurrencyPrecise(summary.totalInvested)}</div>
          <div className="hint">Capital locked at allocation</div>
        </div>
        <div className="metric-card">
          <div className="label">Redemption value</div>
          <div className="value">{formatCurrencyPrecise(summary.totalExpectedReturn)}</div>
          <div className="hint">Face value due at maturity</div>
        </div>
        <div className="metric-card">
          <div className="label">Expected profit</div>
          <div className="value">{formatCurrencyPrecise(totalProfit)}</div>
          <div className="hint">Redemption minus invested</div>
        </div>
        <div className="metric-card">
          <div className="label">Active positions</div>
          <div className="value">{summary.activePositions}</div>
        </div>
        <div className="metric-card">
          <div className="label">Realized yield</div>
          <div className="value">
            {summary.realizedYield != null ? formatPercent(summary.realizedYield) : '—'}
          </div>
          <div className="hint">Average across settled positions</div>
        </div>
      </div>

      {showPricingNote && (
        <div className="alert alert-info" style={{ marginBottom: 'var(--space-xl)' }}>
          Profit is zero when units are priced at face value. Discount is embedded in the unit
          price at listing time, not added on top of invested capital.
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h3>Positions</h3>
        </div>
        {positions.length === 0 ? (
          <div className="empty-state">
            <h3>No positions yet</h3>
            <p>Commit to a live opportunity from the marketplace.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Opportunity</th>
                  <th>Invested</th>
                  <th>Redemption</th>
                  <th>Profit</th>
                  <th>Discount</th>
                  <th>Maturity</th>
                  <th>Status</th>
                  <th>Yield</th>
                </tr>
              </thead>
              <tbody>
                {positions.map((p) => {
                  const profit = positionProfit(p.investedAmount, p.expectedReturn)
                  return (
                    <tr key={p.positionId}>
                      <td>
                        <strong>{p.opportunityTitle}</strong>
                        <div className="num" style={{ fontSize: '0.75rem', marginTop: 4 }}>
                          #{p.opportunityId}
                        </div>
                      </td>
                      <td className="num">{formatCurrencyPrecise(p.investedAmount)}</td>
                      <td className="num">{formatCurrencyPrecise(p.expectedReturn)}</td>
                      <td className={`num${profit > 0 ? ' profit-positive' : ''}`}>
                        {formatCurrencyPrecise(profit)}
                      </td>
                      <td className="num">{formatPercent(p.discountRate)}</td>
                      <td>{formatDate(p.maturityDate)}</td>
                      <td>
                        <PositionStatusBadge status={p.status} />
                      </td>
                      <td className="num">
                        {p.realizedYield != null ? formatPercent(p.realizedYield) : '—'}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  )
}
