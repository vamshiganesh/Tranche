import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  getOpportunity,
  publishOpportunity,
  reviewOpportunity,
  transitionOpportunity,
} from '../../api/opportunities'
import type { OpportunityDetail, OpportunityStatus } from '../../api/types'
import { AuditTimelinePanel } from '../../components/AuditTimeline'
import { RiskBadge, StatusBadge } from '../../components/StatusBadge'
import { ApiClientError } from '../../api/client'
import { useToast } from '../../context/ToastContext'
import {
  formatCurrency,
  formatCurrencyPrecise,
  formatDate,
  formatPercent,
  subscriptionProgress,
} from '../../lib/format'

export function AdminOpportunityDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [opp, setOpp] = useState<OpportunityDetail | null>(null)
  const [comment, setComment] = useState('')
  const [busy, setBusy] = useState(false)
  const [timelineKey, setTimelineKey] = useState(0)

  const opportunityId = Number(id)

  useEffect(() => {
    getOpportunity(opportunityId)
      .then(setOpp)
      .catch(() => navigate('/admin'))
  }, [opportunityId, navigate])

  async function reload() {
    const updated = await getOpportunity(opportunityId)
    setOpp(updated)
    setTimelineKey((k) => k + 1)
  }

  async function runAction(fn: () => Promise<unknown>, success: string) {
    setBusy(true)
    try {
      await fn()
      showToast(success, 'success')
      await reload()
    } catch (err) {
      const msg = err instanceof ApiClientError ? err.message : 'Action failed'
      showToast(msg, 'error')
    } finally {
      setBusy(false)
    }
  }

  if (!opp) {
    return (
      <div className="loading-center">
        <div className="spinner" />
      </div>
    )
  }

  const subscribed = subscriptionProgress(opp.remainingUnits, opp.totalUnits)
  const canReview = opp.status === 'UNDER_REVIEW'
  const canPublish = opp.status === 'APPROVED'
  const canMature =
    opp.status === 'LIVE' || opp.status === 'FULLY_SUBSCRIBED'
  const canSettle = opp.status === 'MATURED'

  return (
    <>
      <p style={{ marginBottom: 'var(--space-md)' }}>
        <Link to="/admin/opportunities">← All opportunities</Link>
      </p>
      <header className="page-header page-header-row">
        <div>
          <h2>{opp.title}</h2>
          <p>
            {opp.issuerName ?? 'Issuer'} · <StatusBadge status={opp.status} />
          </p>
        </div>
      </header>

      <div className="detail-grid">
        <div>
          <div className="card" style={{ marginBottom: 'var(--space-xl)' }}>
            <div className="card-body">
              <div className="detail-section">
                <h4>Overview</h4>
                <p style={{ color: 'var(--ink-secondary)', lineHeight: 1.65 }}>
                  {opp.description ?? 'No description provided.'}
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
                    <dt>Discount</dt>
                    <dd className="mono">{formatPercent(opp.discountRate)}</dd>
                  </div>
                  <div className="kv-item">
                    <dt>Risk</dt>
                    <dd>
                      <RiskBadge grade={opp.riskGrade} />
                    </dd>
                  </div>
                  <div className="kv-item">
                    <dt>Unit price</dt>
                    <dd className="mono">{formatCurrencyPrecise(opp.unitPrice)}</dd>
                  </div>
                  <div className="kv-item">
                    <dt>Units</dt>
                    <dd className="mono">
                      {opp.remainingUnits} / {opp.totalUnits}
                    </dd>
                  </div>
                  <div className="kv-item">
                    <dt>Maturity</dt>
                    <dd>{formatDate(opp.maturityDate)}</dd>
                  </div>
                </dl>
              </div>
              <div className="detail-section">
                <h4>Subscription</h4>
                <div className="progress-bar" style={{ marginBottom: 'var(--space-sm)' }}>
                  <div className="progress-bar-fill" style={{ width: `${subscribed}%` }} />
                </div>
                <p style={{ fontSize: '0.8125rem', color: 'var(--ink-muted)' }}>
                  {subscribed}% subscribed
                </p>
              </div>
              {opp.reviewComment && (
                <div className="detail-section">
                  <h4>Last review note</h4>
                  <p style={{ color: 'var(--ink-secondary)' }}>{opp.reviewComment}</p>
                </div>
              )}
            </div>
          </div>

          <AuditTimelinePanel
            key={timelineKey}
            entityType="Opportunity"
            entityId={opportunityId}
          />
        </div>

        <aside className="card">
          <div className="card-header">
            <h3>Actions</h3>
          </div>
          <div className="card-body action-stack">
            {canReview && (
              <>
                <div className="field">
                  <label htmlFor="reviewComment">Review comment</label>
                  <textarea
                    id="reviewComment"
                    className="input textarea"
                    rows={3}
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    placeholder="Optional note for the issuer"
                  />
                </div>
                <button
                  type="button"
                  className="btn btn-primary btn-block"
                  disabled={busy}
                  onClick={() =>
                    runAction(
                      () => reviewOpportunity(opportunityId, 'APPROVE', comment || undefined),
                      'Opportunity approved'
                    )
                  }
                >
                  Approve
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-block"
                  disabled={busy}
                  onClick={() =>
                    runAction(
                      () => reviewOpportunity(opportunityId, 'REJECT', comment || undefined),
                      'Returned to issuer as draft'
                    )
                  }
                >
                  Reject
                </button>
              </>
            )}

            {canPublish && (
              <button
                type="button"
                className="btn btn-primary btn-block"
                disabled={busy}
                onClick={() =>
                  runAction(() => publishOpportunity(opportunityId), 'Published to marketplace')
                }
              >
                Publish live
              </button>
            )}

            {canMature && (
              <button
                type="button"
                className="btn btn-secondary btn-block"
                disabled={busy}
                onClick={() =>
                  runAction(
                    () => transitionOpportunity(opportunityId, 'MATURED' as OpportunityStatus),
                    'Marked as matured'
                  )
                }
              >
                Mark matured
              </button>
            )}

            {canSettle && (
              <button
                type="button"
                className="btn btn-primary btn-block"
                disabled={busy}
                onClick={() =>
                  runAction(
                    () => transitionOpportunity(opportunityId, 'SETTLED' as OpportunityStatus),
                    'Settlement complete'
                  )
                }
              >
                Settle
              </button>
            )}

            {!canReview && !canPublish && !canMature && !canSettle && (
              <p style={{ color: 'var(--ink-muted)', fontSize: '0.875rem' }}>
                No admin actions available for this status.
              </p>
            )}
          </div>
        </aside>
      </div>
    </>
  )
}
