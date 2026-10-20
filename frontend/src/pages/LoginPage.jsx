import { Button, Card, Form, Input, Typography, message } from 'antd'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const { Title, Text } = Typography

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form] = Form.useForm()

  const onFinish = async (values) => {
    try {
      await login(values.email, values.password)
      message.success('Đăng nhập thành công')
      navigate(location.state?.from || '/', { replace: true })
    } catch (error) {
      const data = error.response ? error.response.data : null
      form.setFields([
        { name: 'password', errors: [data && data.message ? data.message : 'Đăng nhập thất bại'] },
      ])
    }
  }

  return (
    <div style={{ padding: 48, maxWidth: 420, margin: '0 auto' }}>
      <Card>
        <Title level={3}>Đăng nhập tài khoản</Title>
        <Form form={form} layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item
            label="Email"
            name="email"
            rules={[
              { required: true, message: 'Vui lòng nhập email' },
              { type: 'email', message: 'Email không hợp lệ' },
            ]}
          >
            <Input placeholder="Email" autoComplete="email" />
          </Form.Item>
          <Form.Item label="Mật khẩu" name="password" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}>
            <Input.Password placeholder="Mật khẩu" autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            Đăng nhập
          </Button>
        </Form>
        <Text>
          Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
        </Text>
      </Card>
    </div>
  )
}

export default LoginPage
