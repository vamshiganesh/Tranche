import { apiRequest } from './client'
import type { AuditTimeline, PortfolioResponse } from './types'

export function fetchPortfolio() {
  return apiRequest<PortfolioResponse>('/portfolio')
}

export function fetchAuditTimeline(entityType: string, entityId: number) {
  return apiRequest<AuditTimeline>(`/audit/${entityType}/${entityId}`)
}
