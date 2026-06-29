import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { fetchMe, login as apiLogin } from '../api/opportunities'
import { getStoredToken, setStoredToken } from '../api/client'
import type { CurrentUser } from '../api/types'

interface AuthState {
  user: CurrentUser | null
  loading: boolean
  login: (email: string, password: string) => Promise<CurrentUser>
  logout: () => void
  refreshUser: () => Promise<void>
}


const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [loading, setLoading] = useState(true)

  const refreshUser = useCallback(async () => {
    const token = getStoredToken()
    if (!token) {
      setUser(null)
      return
    }
    const me = await fetchMe()
    setUser(me)
  }, [])

  useEffect(() => {
    refreshUser()
      .catch(() => {
        setStoredToken(null)
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [refreshUser])

  const login = useCallback(async (email: string, password: string) => {
    const res = await apiLogin(email, password)
    setStoredToken(res.accessToken)
    const me = await fetchMe()
    setUser(me)
    return me
  }, [])

  const logout = useCallback(() => {
    setStoredToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, logout, refreshUser }),
    [user, loading, login, logout, refreshUser]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
