export type Role = 'ADMIN' | 'ISSUER' | 'INVESTOR'

export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ApiError {
  error: {
    code: string
    message: string
    correlationId: string | null
    timestamp: string
  }
}

export interface User {
  id: string
  email: string
  role: Role
  fullName: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface CurrentUser extends User {
  emailVerified: boolean
  walletBalance?: number
  kycStatus?: VerificationStatus | null
  hasIssuerProfile?: boolean
  issuerVerificationStatus?: VerificationStatus | null
}

export interface RegisterResponse extends User {
  createdAt: string
  emailVerificationRequired: boolean
  devVerificationCode?: string | null
}

export interface IssuerProfile {
  id: number
  companyName: string
  registrationNumber: string | null
  userId: string
  verificationStatus: VerificationStatus
}

export type OpportunityStatus =
  | 'DRAFT'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'LIVE'
  | 'FULLY_SUBSCRIBED'
  | 'MATURED'
  | 'SETTLED'

export type RiskGrade = 'A' | 'B' | 'C' | 'D'

export interface OpportunitySummary {
  id: number
  title: string
  faceValue: number
  discountRate: number
  tenureDays: number
  riskGrade: RiskGrade
  remainingUnits: number
  unitPrice: number
  status: OpportunityStatus
  maturityDate: string | null
}

export interface OpportunityDetail extends OpportunitySummary {
  minimumLot: number
  totalUnits: number
  description: string | null
  issuerId: number
  issuerName: string | null
  createdAt: string
  updatedAt: string
  publishedAt: string | null
  reviewedAt: string | null
  reviewComment: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface OpportunityStatusUpdate {
  id: number
  status: OpportunityStatus
  updatedAt: string
  reviewedAt: string | null
  publishedAt: string | null
  reviewComment: string | null
}

export interface CommitmentResponse {
  orderId: number
  opportunityId: number
  unitsRequested: number
  unitsAllocated: number
  amountRequested: number
  amountAllocated: number
  fillStatus: 'FULL' | 'PARTIAL' | 'REJECTED'
  status: string
  idempotencyKey: string
  createdAt: string
}

export interface PortfolioSummary {
  totalInvested: number
  totalExpectedReturn: number
  activePositions: number
  realizedYield: number | null
}

export interface PortfolioPosition {
  positionId: number
  opportunityId: number
  opportunityTitle: string
  investedAmount: number
  expectedReturn: number
  discountRate: number
  maturityDate: string
  status: 'ACTIVE' | 'MATURED' | 'SETTLED'
  realizedYield: number | null
}

export interface PortfolioResponse {
  summary: PortfolioSummary
  positions: PortfolioPosition[]
}

export interface AuditTimelineEntry {
  id: number
  action: string
  actorRole: string
  actorId: string | null
  beforeState: Record<string, unknown> | null
  afterState: Record<string, unknown> | null
  correlationId: string | null
  createdAt: string
}

export interface AuditTimeline {
  entityType: string
  entityId: number
  timeline: AuditTimelineEntry[]
}

export interface CreateOpportunityRequest {
  title: string
  faceValue: number
  discountRate: number
  tenureDays: number
  minimumLot: number
  riskGrade: RiskGrade
  totalUnits: number
  unitPrice: number
  description?: string
}
