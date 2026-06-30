import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/** Redirects issuers without a company profile to onboarding. */
export function IssuerProfileGate() {
  const { user, loading } = useAuth()
  const location = useLocation()
  const onOnboarding = location.pathname === '/issuer/onboarding'

  if (loading || !user) return null

  if (user.role === 'ISSUER' && !user.hasIssuerProfile && !onOnboarding) {
    return <Navigate to="/issuer/onboarding" replace />
  }

  if (
    user.role === 'ISSUER'
    && user.hasIssuerProfile
    && onOnboarding
    && user.issuerVerificationStatus !== 'REJECTED'
  ) {
    return <Navigate to="/issuer" replace />
  }

  return <Outlet />
}
