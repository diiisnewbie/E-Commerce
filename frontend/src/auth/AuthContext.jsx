import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api from '../api'

const AuthContext = createContext(null)

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('user'))
  } catch {
    return null
  }
}

function persistSession(data) {
  localStorage.setItem('accessToken', data.accessToken)
  localStorage.setItem('refreshToken', data.refreshToken)
  localStorage.setItem('user', JSON.stringify(data.user))
}

function clearStoredSession() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser)
  const [ready, setReady] = useState(() => !localStorage.getItem('accessToken'))

  // Khôi phục phiên khi reload bằng GET /auth/me (7.1) — `ready` đã khởi tạo đúng ở trên
  useEffect(() => {
    if (!localStorage.getItem('accessToken')) {
      return undefined
    }
    let cancelled = false
    api
      .get('/api/v1/auth/me')
      .then((response) => {
        if (!cancelled) {
          const me = response.data.data
          setUser(me)
          localStorage.setItem('user', JSON.stringify(me))
        }
      })
      .catch(() => {
        // 401 đã được interceptor xử lý (clear + redirect); lỗi mạng giữ nguyên phiên hiện có
      })
      .finally(() => {
        if (!cancelled) {
          setReady(true)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  // Đồng bộ phiên: tab khác (storage event) và cùng tab (auth:session-updated từ interceptor)
  useEffect(() => {
    const sync = () => {
      setUser(readStoredUser())
    }
    const onStorage = (event) => {
      if (event.key === null || event.key === 'user' || event.key === 'accessToken') {
        sync()
      }
    }
    window.addEventListener('storage', onStorage)
    window.addEventListener('auth:session-updated', sync)
    return () => {
      window.removeEventListener('storage', onStorage)
      window.removeEventListener('auth:session-updated', sync)
    }
  }, [])

  const login = useCallback(async (email, password) => {
    const response = await api.post('/api/v1/auth/login', { email, password })
    const data = response.data.data
    persistSession(data)
    setUser(data.user)
    return data.user
  }, [])

  const register = useCallback(async ({ email, password, fullName }) => {
    const response = await api.post('/api/v1/auth/register', { email, password, fullName })
    return response.data.data
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.post('/api/v1/auth/logout')
    } catch {
      // Token có thể đã hết hạn — vẫn đăng xuất cục bộ
    }
    clearStoredSession()
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, ready, login, register, logout }), [user, ready, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
