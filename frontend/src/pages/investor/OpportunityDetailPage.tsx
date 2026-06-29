import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getOpportunity, placeCommitment } from '../../api/opportunities'
import type { OpportunityDetail } from '../../api/types'
import { RiskBadge, StatusBadge } from '../../components/StatusBadge'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { ApiClientError } from '../../api/client'
import {
  formatCurrency,
  formatCurrencyPrecise,
  formatDate,
  formatPercent,
  newIdempotencyKey,
  subscriptionProgress,
} from '../../lib/format'

export function OpportunityDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { refreshUser } = useAuth()
  const { showToast } = useToast()
  const [opp, setOpp] = useState<OpportunityDetail | null>(null)
  const [units, setUnits] = useState(1)
  const [modalOpen, setModalOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const opportunityId = Number(id)

  useEffect(() => {
    getOpportunity(opportunityId)
      .then(setOpp)
      .catch(() => navigate('/marketplace'))
  }, [opportunityId, navigate])

  if (!opp) {
    return (
      <div className="loading-center">
        <div className="spinner" />
      </div>
    )
  }

  const amount = units * opp.unitPrice
  const subscribed = subscriptionProgress(opp.remainingUnits, opp.totalUnits)
  const canInvest = opp.status === 'LIVE' && opp.remainingUnits > 0

  async function confirmInvest() {
    setSubmitting(true)
    try {
      const result = await placeCommitment(
        opportunityId,
        units,
        amount,
        newIdempotencyKey()
      )
      await refreshUser()
      setModalOpen(false)
      const fill =
        result.fillStatus === 'PARTIAL'
          ? `Partial fill: ${result.unitsAllocated} of ${result.unitsRequested} units`
          : `Allocated ${result.unitsAllocated} units`
      showToast(fill, result.fillStatus === 'REJECTED' ? 'error' : 'success')
      const updated = await getOpportunity(opportunityId)
      setOpp(updated)
    } catch (err) {
      const msg = err instanceof ApiClientError ? err.message : 'Commitment failed'
      showToast(msg, 'error')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <p style={{ marginBottom: 'var(--space-md)' }}>
        <Link to="/marketplace">← Marketplace</Link>
      </p>
      <header className="page-header">
        <h2>{opp.title}</h2>
        <p>
          {opp.issuerName ?? 'Issuer'} · <StatusBadge status={opp.status} />
        </p>
      </header>

      <div className="detail-grid">
        <div>
          <div className="card" style={{ marginBottom: 'var(--space-xl)' }}>
            <div className="card-body">
              <div className="detail-section">
                <h4>Overview</h4>
                <p style={{ color: 'var(--ink-secondary)', lineHeight: 1.65 }}>
                  {opp.description ??
                    'Trade receivable opportunity listed on the Tranche platform.'}
                </p>
              </div>
              <div className="detail-section">
                <h4>Terms</h4>
                <dl className="kv-grid">
                  <div className="kv-item">
                    <dt>Face value</dt>
                    <dd className="mono">{formatCurrency(opp.faceValue)}</dd>
                  </div>
                  <div className="kv-item">
                    <dt>Discount rate</dt>
                    <dd className="mono">{formatPercent(opp.discountRate)}</dd>
                  </div>
                  <div className="kv-item">
                    <dt>Tenure</dt>
                    <dd>{opp.tenureDays} days</dd>
                  </div>
                  <div className="kv-item">
                    <dt>Maturity</dt>
                    <dd>{formatDate(opp.maturityDate)}</dd>
                  </div>
                  <div className="kv-item">
                    <dt>Unit price</dt>
                    <dd className="mono">{formatCurrencyPrecise(opp.unitPrice)}</dd>
                  </div>
                  <div className="kv-item">
                    <dt>Minimum lot</dt>
                    <dd className="mono">{formatCurrencyPrecise(opp.minimumLot)}</dd>
                  </div>
                  <div className="kv-item">
                    <dt>Risk grade</dt>
                    <dd>
                      <RiskBadge grade={opp.riskGrade} />
                    </dd>
                  </div>
                  <div className="kv-item">
                    <dt>Units remaining</dt>
                    <dd className="mono">
                      {opp.remainingUnits} / {opp.totalUnits}
                    </dd>
                  </div>
                </dl>
              </div>
              <div className="detail-section">
                <h4>Subscription</h4>
                <div className="progress-bar" style={{ marginBottom: 'var(--space-sm)' }}>
                  <div
                    className="progress-bar-fill"
                    style={{ width: `${subscribed}%` }}
                  />
                </div>
                <p style={{ fontSize: '0.8125rem', color: 'var(--ink-muted)' }}>
                  {subscribed}% subscribed · {opp.remainingUnits} units available
                </p>
              </div>
            </div>
          </div>
        </div>

        <aside className="card">
          <div className="card-header">
            <h3>Place commitment</h3>
          </div>
          <div className="card-body">
            {canInvest ? (
              <>
                <div className="field">
                  <label htmlFor="units">Units requested</label>
                  <input
                    id="units"
                    className="input mono"
                    type="number"
                    min={1}
                    max={Math.min(opp.remainingUnits, 100000)}
                    value={units}
                    onChange={(e) => setUnits(Math.max(1, Number(e.target.value)))}
                  />
                </div>
                <div className="kv-item" style={{ marginBottom: 'var(--space-lg)' }}>
                  <dt>Amount</dt>
                  <dd className="mono" style={{ fontSize: '1.25rem' }}>
                    {formatCurrencyPrecise(amount)}
                  </dd>
                </div>
                <button
                  type="button"
                  className="btn btn-primary btn-block"
                  onClick={() => setModalOpen(true)}
                >
                  Review commitment
                </button>
              </>
            ) : (
              <p style={{ color: 'var(--ink-muted)', fontSize: '0.875rem' }}>
                This opportunity is not accepting new commitments.
              </p>
            )}
          </div>
        </aside>
      </div>

      {modalOpen && (
        <div className="modal-backdrop" onClick={() => !submitting && setModalOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} role="dialog">
            <div className="modal-header">
              <h3>Confirm commitment</h3>
            </div>
            <div className="modal-body">
              <p style={{ marginBottom: 'var(--space-md)', color: 'var(--ink-secondary)' }}>
                Funds will be locked immediately upon allocation. Retries use a fresh
                idempotency key to prevent duplicate orders.
              </p>
              <dl className="kv-grid">
                <div className="kv-item">
                  <dt>Units</dt>
                  <dd className="mono">{units}</dd>
                </div>
                <div className="kv-item">
                  <dt>Amount</dt>
                  <dd className="mono">{formatCurrencyPrecise(amount)}</dd>
                </div>
              </dl>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                disabled={submitting}
                onClick={() => setModalOpen(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                disabled={submitting}
                onClick={confirmInvest}
              >
                {submitting ? 'Submitting…' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
