import { apiRequest } from './client'
import type { CurrentUser, LoginResponse, RegisterResponse, Role } from './types'

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

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
  role: Role
}

export function register(body: RegisterRequest) {
  return apiRequest<RegisterResponse>('/auth/register', {
    method: 'POST',
    body,
    auth: false,
  })
}

export function verifyEmail(email: string, code: string) {
  return apiRequest<void>('/auth/verify-email', {
    method: 'POST',
    body: { email, code },
    auth: false,
  })
}

export function resendVerification(email: string) {
  return apiRequest<{ devVerificationCode?: string; message?: string }>(
    '/auth/resend-verification',
    {
      method: 'POST',
      body: { email },
      auth: false,
    }
  )
}
