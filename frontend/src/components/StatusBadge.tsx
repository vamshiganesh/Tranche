import type { OpportunityStatus, RiskGrade } from '../api/types'

const statusClass: Record<OpportunityStatus, string> = {
  DRAFT: 'badge-draft',
  UNDER_REVIEW: 'badge-review',
  APPROVED: 'badge-approved',
  LIVE: 'badge-live',
  FULLY_SUBSCRIBED: 'badge-full',
  MATURED: 'badge-matured',
  SETTLED: 'badge-settled',
}

const statusLabel: Record<OpportunityStatus, string> = {
  DRAFT: 'Draft',
  UNDER_REVIEW: 'In review',
  APPROVED: 'Approved',
  LIVE: 'Live',
  FULLY_SUBSCRIBED: 'Fully subscribed',
  MATURED: 'Matured',
  SETTLED: 'Settled',
}

export function StatusBadge({ status }: { status: OpportunityStatus }) {
  return <span className={`badge ${statusClass[status]}`}>{statusLabel[status]}</span>
}

export function RiskBadge({ grade }: { grade: RiskGrade }) {
  return <span className={`badge badge-grade-${grade.toLowerCase()}`}>Grade {grade}</span>
}

export function PositionStatusBadge({ status }: { status: string }) {
  const cls =
    status === 'SETTLED' ? 'badge-settled' : status === 'MATURED' ? 'badge-matured' : 'badge-active'
  return <span className={`badge ${cls}`}>{status}</span>
}
