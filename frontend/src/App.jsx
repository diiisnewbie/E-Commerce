import { useEffect, useState } from 'react'
import { Alert, Button, Card, Layout, Space, Spin, Typography } from 'antd'
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { CheckCircleOutlined, CloseCircleOutlined, LogoutOutlined } from '@ant-design/icons'
import { getHealth } from './api'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { RequireAdmin, RequireAuth } from './auth/RequireAuth'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProfilePage from './pages/ProfilePage'
import AdminPage from './pages/AdminPage'

const { Header, Content } = Layout
const { Title, Text } = Typography

function HealthPage() {
  const [health, setHealth] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    getHealth()
      .then((response) => {
        if (!cancelled) {
          setHealth(response.data)
          setError(null)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('Không kết nối được backend')
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin size="large" tip="Đang kiểm tra trạng thái..." />
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ padding: 48 }}>
        <Alert
          message="Backend DOWN"
          description={error}
          type="error"
          showIcon
          icon={<CloseCircleOutlined />}
        />
      </div>
    )
  }

  const isUp = health?.data?.status === 'UP'

  return (
    <div style={{ padding: 48 }}>
      <Card>
        <Title level={3}>
          {isUp ? (
            <CheckCircleOutlined style={{ color: '#52c41a' }} />
          ) : (
            <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
          )}{' '}
          Backend: {health?.data?.status}
        </Title>
        <Text>Service: {health?.data?.service}</Text>
        <br />
        <Text>Timestamp: {health?.timestamp}</Text>
      </Card>
    </div>
  )
}

function AppHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const onLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <Header
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingInline: 24,
      }}
    >
      <Space size="middle">
        <Link to="/" style={{ color: '#fff', fontWeight: 700 }}>
          E-Commerce
        </Link>
        {user && (
          <Link to="/profile" style={{ color: '#fff' }}>
            Hồ sơ
          </Link>
        )}
        {user?.role === 'ADMIN' && (
          <Link to="/admin" style={{ color: '#fff' }}>
            Quản trị
          </Link>
        )}
      </Space>
      <Space size="middle">
        {user ? (
          <>
            <Text style={{ color: '#fff' }}>{user.fullName || user.email}</Text>
            <Button
              size="small"
              icon={<LogoutOutlined />}
              onClick={onLogout}
              aria-label="Đăng xuất"
            >
              Đăng xuất
            </Button>
          </>
        ) : (
          <>
            <Link to="/login" style={{ color: '#fff' }}>
              Đăng nhập
            </Link>
            <Link to="/register" style={{ color: '#fff' }}>
              Đăng ký
            </Link>
          </>
        )}
      </Space>
    </Header>
  )
}

/** Lắng nghe sự kiện redirect do api.js phát ra khi phiên kết thúc (401 không khôi phục được). */
function NavigateListener() {
  const navigate = useNavigate()

  useEffect(() => {
    const handler = (event) => navigate(event.detail, { replace: true })
    window.addEventListener('auth:navigate', handler)
    return () => window.removeEventListener('auth:navigate', handler)
  }, [navigate])

  return null
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <NavigateListener />
        <Layout style={{ minHeight: '100vh' }}>
          <AppHeader />
          <Content>
            <Routes>
              <Route path="/" element={<HealthPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route
                path="/profile"
                element={
                  <RequireAuth>
                    <ProfilePage />
                  </RequireAuth>
                }
              />
              <Route
                path="/admin"
                element={
                  <RequireAdmin>
                    <AdminPage />
                  </RequireAdmin>
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Content>
        </Layout>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
