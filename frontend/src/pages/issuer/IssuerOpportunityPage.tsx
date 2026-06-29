import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getOpportunity, submitOpportunity } from '../../api/opportunities'
import type { OpportunityDetail, OpportunityStatus } from '../../api/types'
import { RiskBadge, StatusBadge } from '../../components/StatusBadge'
import { ApiClientError } from '../../api/client'
import { useToast } from '../../context/ToastContext'
import {
  formatCurrency,
  formatCurrencyPrecise,
  formatDate,
  formatDateTime,
  formatPercent,
  subscriptionProgress,
} from '../../lib/format'

const STATUS_GUIDANCE: Record<
  OpportunityStatus,
  { headline: string; detail: string }
> = {
  DRAFT: {
    headline: 'Ready to submit',
    detail: 'Review the terms below, then submit for platform review when ready.',
  },
  UNDER_REVIEW: {
    headline: 'Under admin review',
    detail: 'Your opportunity is in the review queue. You will be notified when it is approved or returned.',
  },
  APPROVED: {
    headline: 'Approved, awaiting publish',
    detail: 'An admin has approved this invoice. It will appear on the marketplace once published.',
  },
  LIVE: {
    headline: 'Live on marketplace',
    detail: 'Investors can place commitments. Funding progress updates in real time.',
  },
  FULLY_SUBSCRIBED: {
    headline: 'Fully subscribed',
    detail: 'All units have been allocated. No further commitments will be accepted.',
  },
  MATURED: {
    headline: 'Matured',
    detail: 'The tenure has ended. Settlement is handled by the platform admin.',
  },
  SETTLED: {
    headline: 'Settled',
    detail: 'This opportunity has completed its lifecycle.',
  },
}

export function IssuerOpportunityPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [opp, setOpp] = useState<OpportunityDetail | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const opportunityId = Number(id)

  useEffect(() => {
    getOpportunity(opportunityId)
      .then(setOpp)
      .catch(() => navigate('/issuer'))
  }, [opportunityId, navigate])

  async function handleSubmitForReview() {
    setSubmitting(true)
    try {
      await submitOpportunity(opportunityId)
      showToast('Submitted for platform review', 'success')
      const updated = await getOpportunity(opportunityId)
      setOpp(updated)
    } catch (err) {
      const msg = err instanceof ApiClientError ? err.message : 'Submit failed'
      showToast(msg, 'error')
    } finally {
      setSubmitting(false)
    }
  }

  if (!opp) {
    return (
      <div className="loading-center">
        <div className="spinner" />
      </div>
    )
  }

  const guidance = STATUS_GUIDANCE[opp.status]
  const unitsSold = opp.totalUnits - opp.remainingUnits
  const subscribedPct = subscriptionProgress(opp.remainingUnits, opp.totalUnits)
  const amountRaised = unitsSold * opp.unitPrice
  const canSubmit = opp.status === 'DRAFT'
  const isFunding =
    opp.status === 'LIVE' ||
    opp.status === 'FULLY_SUBSCRIBED' ||
    opp.status === 'MATURED' ||
    opp.status === 'SETTLED'

  return (
    <>
      <p style={{ marginBottom: 'var(--space-md)' }}>
        <Link to="/issuer">← My invoices</Link>
      </p>
      <header className="page-header">
        <h2>{opp.title}</h2>
        <p>
          <StatusBadge status={opp.status} />
          <RiskBadge grade={opp.riskGrade} />
        </p>
      </header>

      <div className="detail-grid">
        <div>
          {isFunding && (
            <div className="metrics-row" style={{ marginBottom: 'var(--space-xl)' }}>
              <div className="metric-card">
                <div className="label">Units sold</div>
                <div className="value">
                  {unitsSold}
                  <span style={{ fontSize: '0.875rem', color: 'var(--ink-muted)' }}>
                    {' '}
                    / {opp.totalUnits}
                  </span>
                </div>
              </div>
              <div className="metric-card">
                <div className="label">Amount raised</div>
                <div className="value">{formatCurrencyPrecise(amountRaised)}</div>
              </div>
              <div className="metric-card">
                <div className="label">Face value</div>
                <div className="value">{formatCurrency(opp.faceValue)}</div>
              </div>
            </div>
          )}

          <div className="card" style={{ marginBottom: 'var(--space-xl)' }}>
            <div className="card-body">
              {opp.description && (
                <div className="detail-section">
                  <h4>Description</h4>
                  <p style={{ color: 'var(--ink-secondary)', lineHeight: 1.65 }}>
                    {opp.description}
                  </p>
                </div>
              )}
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
                </dl>
              </div>
              {isFunding && (
                <div className="detail-section">
                  <h4>Subscription</h4>
                  <div className="progress-bar" style={{ marginBottom: 'var(--space-sm)' }}>
                    <div className="progress-bar-fill" style={{ width: `${subscribedPct}%` }} />
                  </div>
                  <p style={{ fontSize: '0.8125rem', color: 'var(--ink-muted)' }}>
                    {subscribedPct}% subscribed · {opp.remainingUnits} units remaining
                  </p>
                </div>
              )}
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h3>Timeline</h3>
            </div>
            <div className="card-body">
              <dl className="kv-grid">
                <div className="kv-item">
                  <dt>Created</dt>
                  <dd>{formatDateTime(opp.createdAt)}</dd>
                </div>
                {opp.reviewedAt && (
                  <div className="kv-item">
                    <dt>Reviewed</dt>
                    <dd>{formatDateTime(opp.reviewedAt)}</dd>
                  </div>
                )}
                {opp.publishedAt && (
                  <div className="kv-item">
                    <dt>Published</dt>
                    <dd>{formatDateTime(opp.publishedAt)}</dd>
                  </div>
                )}
                <div className="kv-item">
                  <dt>Last updated</dt>
                  <dd>{formatDateTime(opp.updatedAt)}</dd>
                </div>
              </dl>
            </div>
          </div>
        </div>

        <aside className="card">
          <div className="card-header">
            <h3>Status</h3>
          </div>
          <div className="card-body action-stack">
            <div className="status-callout">
              <strong>{guidance.headline}</strong>
              <p>{guidance.detail}</p>
            </div>

            {opp.reviewComment && (
              <div className="review-note">
                <h4>Review note</h4>
                <p>{opp.reviewComment}</p>
              </div>
            )}

            {canSubmit && (
              <button
                type="button"
                className="btn btn-primary btn-block"
                disabled={submitting}
                onClick={handleSubmitForReview}
              >
                {submitting ? 'Submitting…' : 'Submit for review'}
              </button>
            )}

            {opp.status === 'UNDER_REVIEW' && (
              <p className="text-muted">No action required while under review.</p>
            )}

            {(opp.status === 'LIVE' || opp.status === 'FULLY_SUBSCRIBED') && (
              <p className="text-muted">
                Investor commitments are managed by the platform. Track funding progress above.
              </p>
            )}
          </div>
        </aside>
      </div>
    </>
  )
}
