import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { Role } from '../api/types'

export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="loading-center" style={{ minHeight: '100vh' }}>
        <div className="spinner" />
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (roles && !roles.includes(user.role)) {
    const dest =
      user.role === 'ADMIN' ? '/admin' : user.role === 'ISSUER' ? '/issuer' : '/marketplace'
    return <Navigate to={dest} replace />
  }

  return <Outlet />
}
