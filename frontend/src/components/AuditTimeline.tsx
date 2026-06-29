import { useEffect, useState } from 'react'
import { fetchAuditTimeline } from '../api/audit'
import type { AuditTimelineEntry } from '../api/types'
import { formatAction, formatDateTime } from '../lib/format'

export function AuditTimelinePanel({
  entityType,
  entityId,
}: {
  entityType: string
  entityId: number
}) {
  const [entries, setEntries] = useState<AuditTimelineEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchAuditTimeline(entityType, entityId)
      .then((res) => setEntries(res.timeline))
      .catch(() => setError('Could not load audit trail'))
      .finally(() => setLoading(false))
  }, [entityType, entityId])

  if (loading) {
    return (
      <div className="card">
        <div className="card-header">
          <h3>Audit trail</h3>
        </div>
        <div className="card-body loading-center" style={{ minHeight: 120 }}>
          <div className="spinner" />
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="card">
        <div className="card-header">
          <h3>Audit trail</h3>
        </div>
        <div className="card-body">
          <p className="text-muted">{error}</p>
        </div>
      </div>
    )
  }

  if (entries.length === 0) {
    return (
      <div className="card">
        <div className="card-header">
          <h3>Audit trail</h3>
        </div>
        <div className="empty-state compact">
          <p>No audit events recorded yet.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="card">
      <div className="card-header">
        <h3>Audit trail</h3>
      </div>
      <div className="card-body">
        <ol className="timeline">
          {entries.map((e) => (
            <li key={e.id} className="timeline-item">
              <div className="timeline-marker" />
              <div className="timeline-content">
                <div className="timeline-head">
                  <strong>{formatAction(e.action)}</strong>
                  <time>{formatDateTime(e.createdAt)}</time>
                </div>
                <p className="timeline-meta">
                  {e.actorRole}
                  {e.actorId ? ` · ${e.actorId.slice(0, 8)}…` : ''}
                  {e.correlationId && (
                    <span className="mono" style={{ marginLeft: 8 }}>
                      {e.correlationId.slice(0, 8)}
                    </span>
                  )}
                </p>
                {e.afterState?.status != null && (
                  <p className="timeline-state">
                    Status → <span className="mono">{String(e.afterState.status)}</span>
                  </p>
                )}
              </div>
            </li>
          ))}
        </ol>
      </div>
    </div>
  )
}
