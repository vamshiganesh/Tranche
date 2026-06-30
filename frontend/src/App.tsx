import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { IssuerProfileGate } from './components/IssuerProfileGate'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'
import { AdminOnboardingPage } from './pages/admin/AdminOnboardingPage'
import { AdminOpportunitiesPage } from './pages/admin/AdminOpportunitiesPage'
import { AdminOpportunityDetailPage } from './pages/admin/AdminOpportunityDetailPage'
import { AdminReviewQueuePage } from './pages/admin/AdminReviewQueuePage'
import { CreateOpportunityPage } from './pages/issuer/CreateOpportunityPage'
import { IssuerDashboardPage } from './pages/issuer/IssuerDashboardPage'
import { IssuerOnboardingPage } from './pages/issuer/IssuerOnboardingPage'
import { IssuerOpportunityPage } from './pages/issuer/IssuerOpportunityPage'
import { MarketplacePage } from './pages/investor/MarketplacePage'
import { OpportunityDetailPage } from './pages/investor/OpportunityDetailPage'
import { PortfolioPage } from './pages/investor/PortfolioPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />

            <Route element={<ProtectedRoute />}>
              <Route element={<ProtectedRoute roles={['INVESTOR']} />}>
                <Route element={<AppShell title="Investor workspace" />}>
                  <Route path="/marketplace" element={<MarketplacePage />} />
                  <Route path="/marketplace/:id" element={<OpportunityDetailPage />} />
                  <Route path="/portfolio" element={<PortfolioPage />} />
                </Route>
              </Route>

              <Route element={<ProtectedRoute roles={['ISSUER']} />}>
                <Route element={<IssuerProfileGate />}>
                  <Route element={<AppShell title="Issuer workspace" />}>
                    <Route path="/issuer/onboarding" element={<IssuerOnboardingPage />} />
                    <Route path="/issuer" element={<IssuerDashboardPage />} />
                    <Route path="/issuer/new" element={<CreateOpportunityPage />} />
                    <Route path="/issuer/:id" element={<IssuerOpportunityPage />} />
                  </Route>
                </Route>
              </Route>

              <Route element={<ProtectedRoute roles={['ADMIN']} />}>
                <Route element={<AppShell title="Operations" />}>
                  <Route path="/admin" element={<AdminReviewQueuePage />} />
                  <Route path="/admin/onboarding" element={<AdminOnboardingPage />} />
                  <Route path="/admin/opportunities" element={<AdminOpportunitiesPage />} />
                  <Route
                    path="/admin/opportunities/:id"
                    element={<AdminOpportunityDetailPage />}
                  />
                </Route>
              </Route>

              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
