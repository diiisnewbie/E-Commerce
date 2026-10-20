import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import api, { getHealth } from '../api'

vi.mock('../api', () => ({
  getHealth: vi.fn(),
  default: { get: vi.fn(), post: vi.fn() },
}))

const MOCK_USER = {
  id: 1,
  email: 'user@example.com',
  fullName: 'User Test',
  role: 'USER',
}

const LOGIN_RESPONSE = {
  data: {
    success: true,
    data: {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresInSeconds: 900,
      user: MOCK_USER,
    },
    message: 'Login successful',
    errorCode: null,
    timestamp: '2026-01-01T00:00:00Z',
  },
}

function mockHealthUp() {
  getHealth.mockResolvedValue({
    data: {
      success: true,
      data: { status: 'UP', service: 'ecommerce-backend' },
      message: 'Service is healthy',
      errorCode: null,
      timestamp: '2026-01-01T00:00:00Z',
    },
  })
}

beforeEach(() => {
  localStorage.clear()
  vi.clearAllMocks()
  mockHealthUp()
})

describe('FE-AUTH-01: LoginPage render, submit gọi login, hiển thị user sau login', () => {
  it('submit form đăng nhập gọi API và hiển thị user ở header', async () => {
    window.history.pushState({}, '', '/login')
    api.post.mockResolvedValue(LOGIN_RESPONSE)

    render(<App />)

    const heading = await screen.findByRole('heading', { name: 'Đăng nhập tài khoản' })
    expect(heading).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('Email'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByPlaceholderText('Mật khẩu'), {
      target: { value: 'MatKhau123' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/v1/auth/login', {
        email: 'user@example.com',
        password: 'MatKhau123',
      })
    })

    await waitFor(() => {
      expect(screen.getByText('User Test')).toBeInTheDocument()
    })

    // Phiên được persist xuống localStorage (D-1-10)
    expect(localStorage.getItem('accessToken')).toBe('access-token')
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token')
  })
})

describe('FE-AUTH-02: RequireAuth redirect khi chưa có phiên', () => {
  it('chưa đăng nhập vào /profile → redirect /login', async () => {
    window.history.pushState({}, '', '/profile')

    render(<App />)

    await waitFor(() => {
      expect(window.location.pathname).toBe('/login')
    })
    expect(await screen.findByRole('heading', { name: 'Đăng nhập tài khoản' })).toBeInTheDocument()
  })

  it('mock đã đăng nhập (localStorage + GET /auth/me) → vào được /profile', async () => {
    localStorage.setItem('accessToken', 'access-token')
    localStorage.setItem('refreshToken', 'refresh-token')
    localStorage.setItem('user', JSON.stringify(MOCK_USER))
    api.get.mockResolvedValue({
      data: {
        success: true,
        data: MOCK_USER,
        message: 'Profile retrieved successfully',
        errorCode: null,
        timestamp: '2026-01-01T00:00:00Z',
      },
    })
    window.history.pushState({}, '', '/profile')

    render(<App />)

    expect(await screen.findByText('user@example.com')).toBeInTheDocument()
    expect(window.location.pathname).toBe('/profile')
    expect(api.get).toHaveBeenCalledWith('/api/v1/auth/me')
  })
})
