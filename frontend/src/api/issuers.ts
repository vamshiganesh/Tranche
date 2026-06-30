import { apiRequest } from './client'
import type { IssuerProfile } from './types'

export function getIssuerProfile() {
  return apiRequest<IssuerProfile>('/issuers/profile')
}

export function createIssuerProfile(companyName: string, registrationNumber?: string) {
  return apiRequest<IssuerProfile>('/issuers/profile', {
    method: 'POST',
    body: { companyName, registrationNumber: registrationNumber || null },
  })
}

export function resubmitIssuerProfile(companyName: string, registrationNumber?: string) {
  return apiRequest<IssuerProfile>('/issuers/profile', {
    method: 'PUT',
    body: { companyName, registrationNumber: registrationNumber || null },
  })
}
