import type { ApiError } from './types'

const TOKEN_KEY = 'tranche_token'

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export class ApiClientError extends Error {
  readonly code: string
  readonly status: number
  readonly correlationId: string | null

  constructor(status: number, body: ApiError['error']) {
    super(body.message)
    this.name = 'ApiClientError'
    this.code = body.code
    this.status = status
    this.correlationId = body.correlationId
  }
}

type RequestOptions = {
  method?: string
  body?: unknown
  auth?: boolean
  idempotencyKey?: string
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { method = 'GET', body, auth = true, idempotencyKey } = options

  const headers: Record<string, string> = {
    Accept: 'application/json',
  }

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  if (auth) {
    const token = getStoredToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
  }

  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey
  }

  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    if (data?.error) {
      throw new ApiClientError(response.status, data.error)
    }
    throw new Error(response.statusText || 'Request failed')
  }

  return data as T
}
