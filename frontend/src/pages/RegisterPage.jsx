import { Button, Card, Form, Input, Typography, message } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const { Title, Text } = Typography

function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const onFinish = async (values) => {
    try {
      await register({ email: values.email, password: values.password, fullName: values.fullName })
      message.success('Đăng ký thành công — vui lòng đăng nhập')
      navigate('/login', { replace: true })
    } catch (error) {
      const data = error.response ? error.response.data : null
      const messageText = data && data.message ? data.message : 'Đăng ký thất bại'
      if (data && data.errorCode === 'EMAIL_ALREADY_EXISTS') {
        form.setFields([{ name: 'email', errors: [messageText] }])
      } else {
        message.error(messageText)
      }
    }
  }

  return (
    <div style={{ padding: 48, maxWidth: 420, margin: '0 auto' }}>
      <Card>
        <Title level={3}>Đăng ký tài khoản</Title>
        <Form form={form} layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item
            label="Họ và tên"
            name="fullName"
            rules={[
              { required: true, message: 'Vui lòng nhập họ và tên' },
              { min: 2, max: 100, message: 'Họ và tên 2-100 ký tự' },
            ]}
          >
            <Input placeholder="Họ và tên" autoComplete="name" />
          </Form.Item>
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
          <Form.Item
            label="Mật khẩu"
            name="password"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu' },
              { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' },
            ]}
          >
            <Input.Password placeholder="Mật khẩu" autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            label="Xác nhận mật khẩu"
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: 'Vui lòng xác nhận mật khẩu' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận không khớp'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="Xác nhận mật khẩu" autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            Đăng ký
          </Button>
        </Form>
        <Text>
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </Text>
      </Card>
    </div>
  )
}

export default RegisterPage
