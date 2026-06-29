import { apiRequest } from './client'
import type { AuditTimeline } from './types'


export function fetchAuditTimeline(entityType: string, entityId: number) {
  return apiRequest<AuditTimeline>(`/audit/${entityType}/${entityId}`)
}
