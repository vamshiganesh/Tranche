import { apiRequest } from './client'
import type {
  CommitmentResponse,
  CreateOpportunityRequest,
  CurrentUser,
  LoginResponse,
  OpportunityDetail,
  OpportunityStatus,
  OpportunityStatusUpdate,
  OpportunitySummary,
  PageResponse,
} from './types'

export function login(email: string, password: string) {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: { email, password },
    auth: false,
  })
}

export function fetchMe() {
  return apiRequest<CurrentUser>('/auth/me')
}

export function listOpportunities(params?: {
  status?: OpportunityStatus
  riskGrade?: string
  page?: number
  size?: number
}) {
  const search = new URLSearchParams()
  if (params?.status) search.set('status', params.status)
  if (params?.riskGrade) search.set('riskGrade', params.riskGrade)
  if (params?.page !== undefined) search.set('page', String(params.page))
  if (params?.size !== undefined) search.set('size', String(params.size))
  const qs = search.toString()
  return apiRequest<PageResponse<OpportunitySummary>>(
    `/opportunities${qs ? `?${qs}` : ''}`
  )
}

export function getOpportunity(id: number) {
  return apiRequest<OpportunityDetail>(`/opportunities/${id}`)
}

export function createOpportunity(body: CreateOpportunityRequest) {
  return apiRequest<OpportunityDetail>('/opportunities', {
    method: 'POST',
    body,
  })
}

export function submitOpportunity(id: number) {
  return apiRequest<OpportunityStatusUpdate>(`/opportunities/${id}/submit`, {
    method: 'POST',
  })
}

export function reviewOpportunity(
  id: number,
  action: 'APPROVE' | 'REJECT',
  comment?: string
) {
  return apiRequest<OpportunityStatusUpdate>(`/opportunities/${id}/review`, {
    method: 'POST',
    body: { action, comment },
  })
}

export function publishOpportunity(id: number) {
  return apiRequest<OpportunityStatusUpdate>(`/opportunities/${id}/publish`, {
    method: 'POST',
  })
}

export function transitionOpportunity(id: number, targetStatus: OpportunityStatus) {
  return apiRequest<OpportunityStatusUpdate>(`/opportunities/${id}/transition`, {
    method: 'POST',
    body: { targetStatus },
  })
}

export function placeCommitment(
  opportunityId: number,
  unitsRequested: number,
  amount: number,
  idempotencyKey: string
) {
  return apiRequest<CommitmentResponse>(
    `/opportunities/${opportunityId}/commitments`,
    {
      method: 'POST',
      body: { unitsRequested, amount },
      idempotencyKey,
    }
  )
}
