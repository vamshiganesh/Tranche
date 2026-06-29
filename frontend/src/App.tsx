import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { ProtectedRoute, RootRedirect } from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import { LoginPage } from './pages/LoginPage'
import { AdminOpportunitiesPage } from './pages/admin/AdminOpportunitiesPage'
import { AdminOpportunityDetailPage } from './pages/admin/AdminOpportunityDetailPage'
import { AdminReviewQueuePage } from './pages/admin/AdminReviewQueuePage'
import { CreateOpportunityPage } from './pages/issuer/CreateOpportunityPage'
import { IssuerDashboardPage } from './pages/issuer/IssuerDashboardPage'
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
            <Route path="/login" element={<LoginPage />} />

            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<RootRedirect />} />

              <Route element={<ProtectedRoute roles={['INVESTOR']} />}>
                <Route element={<AppShell title="Investor workspace" />}>
                  <Route path="/marketplace" element={<MarketplacePage />} />
                  <Route path="/marketplace/:id" element={<OpportunityDetailPage />} />
                  <Route path="/portfolio" element={<PortfolioPage />} />
                </Route>
              </Route>

              <Route element={<ProtectedRoute roles={['ISSUER']} />}>
                <Route element={<AppShell title="Issuer workspace" />}>
                  <Route path="/issuer" element={<IssuerDashboardPage />} />
                  <Route path="/issuer/new" element={<CreateOpportunityPage />} />
                  <Route path="/issuer/:id" element={<IssuerOpportunityPage />} />
                </Route>
              </Route>

              <Route element={<ProtectedRoute roles={['ADMIN']} />}>
                <Route element={<AppShell title="Operations" />}>
                  <Route path="/admin" element={<AdminReviewQueuePage />} />
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
