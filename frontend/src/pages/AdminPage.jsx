import { useEffect, useState } from 'react'
import { Alert, Card, Spin, Typography } from 'antd'
import api from '../api'

const { Title, Text } = Typography

function AdminPage() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/v1/admin/stats')
      .then((response) => {
        if (!cancelled) {
          setStats(response.data.data)
          setError(null)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('Không tải được thống kê admin')
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

  return (
    <div style={{ padding: 48, maxWidth: 640, margin: '0 auto' }}>
      <Card>
        <Title level={3}>Quản trị</Title>
        {loading && <Spin size="large" />}
        {!loading && error && <Alert type="error" message={error} showIcon />}
        {!loading && !error && (
          <Text>
            Tổng số người dùng: <strong>{stats?.totalUsers}</strong>
          </Text>
        )}
      </Card>
    </div>
  )
}

export default AdminPage
