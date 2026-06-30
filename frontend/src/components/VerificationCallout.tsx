import type { VerificationStatus } from '../api/types'

const statusClass: Record<VerificationStatus, string> = {
  PENDING: 'badge-review',
  APPROVED: 'badge-approved',
  REJECTED: 'badge-rejected',
}

const statusLabel: Record<VerificationStatus, string> = {
  PENDING: 'Pending review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
}

export function VerificationStatusBadge({ status }: { status: VerificationStatus }) {
  return <span className={`badge ${statusClass[status]}`}>{statusLabel[status]}</span>
}

interface VerificationCalloutProps {
  status: VerificationStatus
  title: string
  children: React.ReactNode
  action?: React.ReactNode
}

export function VerificationCallout({ status, title, children, action }: VerificationCalloutProps) {
  const variant =
    status === 'REJECTED' ? 'verification-callout verification-callout-rejected'
    : status === 'APPROVED' ? 'verification-callout verification-callout-approved'
    : 'verification-callout verification-callout-pending'

  return (
    <div className={variant}>
      <div className="verification-callout-head">
        <div>
          <h4>{title}</h4>
          <VerificationStatusBadge status={status} />
        </div>
      </div>
      <p>{children}</p>
      {action && <div className="verification-callout-action">{action}</div>}
    </div>
  )
}
