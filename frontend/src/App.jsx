import { useEffect, useState } from 'react'
import { Alert, Card, Spin, Typography } from 'antd'
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import { getHealth } from './api'

const { Title, Text } = Typography

function App() {
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

export default App
