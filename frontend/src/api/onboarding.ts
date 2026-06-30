import { apiRequest } from './client'
import type { VerificationStatus } from './types'

export interface PendingInvestor {
  userId: string
  email: string
  fullName: string
  kycStatus: VerificationStatus
  registeredAt: string
}

export interface PendingIssuer {
  userId: string
  email: string
  fullName: string
  companyName: string
  registrationNumber: string | null
  verificationStatus: VerificationStatus
  registeredAt: string
}

export function listPendingInvestors() {
  return apiRequest<PendingInvestor[]>('/admin/onboarding/investors')
}

export function listPendingIssuers() {
  return apiRequest<PendingIssuer[]>('/admin/onboarding/issuers')
}

export function approveInvestorKyc(userId: string) {
  return apiRequest<{ userId: string; status: VerificationStatus }>(
    `/admin/onboarding/investors/${userId}/approve`,
    { method: 'POST', body: {} }
  )
}

export function rejectInvestorKyc(userId: string) {
  return apiRequest<{ userId: string; status: VerificationStatus }>(
    `/admin/onboarding/investors/${userId}/reject`,
    { method: 'POST', body: {} }
  )
}

export function approveIssuerKyb(userId: string) {
  return apiRequest<{ userId: string; status: VerificationStatus }>(
    `/admin/onboarding/issuers/${userId}/approve`,
    { method: 'POST', body: {} }
  )
}

export function rejectIssuerKyb(userId: string) {
  return apiRequest<{ userId: string; status: VerificationStatus }>(
    `/admin/onboarding/issuers/${userId}/reject`,
    { method: 'POST', body: {} }
  )
}
