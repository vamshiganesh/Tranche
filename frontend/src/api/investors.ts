import { apiRequest } from './client'

export function applyDemoCredit() {
  return apiRequest<{ creditedAmount: number; walletBalance: number }>(
    '/investors/wallet/demo-credit',
    { method: 'POST', body: {} }
  )
}

export function resubmitKyc() {
  return apiRequest<void>('/investors/kyc/resubmit', { method: 'POST', body: {} })
}
