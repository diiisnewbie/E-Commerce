import { Spin } from 'antd'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

function Loading() {
  return (
    <div style={{ padding: 48, textAlign: 'center' }}>
      <Spin size="large" />
    </div>
  )
}

/** Chưa đăng nhập → chuyển /login, giữ đường dẫn gốc để đăng nhập xong quay lại. */
export function RequireAuth({ children }) {
  const { user, ready } = useAuth()
  const location = useLocation()

  if (!ready) {
    return <Loading />
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}

/** Vai trò khác ADMIN → chuyển về /. */
export function RequireAdmin({ children }) {
  const { user, ready } = useAuth()

  if (!ready) {
    return <Loading />
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }
  return children
}
