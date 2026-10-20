import { Card, Descriptions, Typography } from 'antd'
import { useAuth } from '../auth/AuthContext'

const { Title } = Typography

function ProfilePage() {
  const { user } = useAuth()

  return (
    <div style={{ padding: 48, maxWidth: 640, margin: '0 auto' }}>
      <Card>
        <Title level={3}>Hồ sơ cá nhân</Title>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="Email">{user?.email}</Descriptions.Item>
          <Descriptions.Item label="Họ và tên">{user?.fullName}</Descriptions.Item>
          <Descriptions.Item label="Vai trò">{user?.role}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  )
}

export default ProfilePage
