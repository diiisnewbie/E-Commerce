import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import App from './App'
import * as api from './api'

vi.mock('./api', () => ({
  getHealth: vi.fn(),
  default: {},
}))

describe('FE-SMOKE-01: App render và hiển thị trạng thái health', () => {
  it('hiển thị trạng thái UP khi health endpoint trả về hợp lệ', async () => {
    api.getHealth.mockResolvedValue({
      data: {
        success: true,
        data: { status: 'UP', service: 'ecommerce-backend' },
        message: 'Service is healthy',
        errorCode: null,
        timestamp: '2026-01-01T00:00:00Z',
      },
    })

    render(<App />)

    await waitFor(() => {
      expect(screen.getByText(/Backend: UP/)).toBeInTheDocument()
    })

    expect(screen.getByText('Service: ecommerce-backend')).toBeInTheDocument()
    expect(api.getHealth).toHaveBeenCalledTimes(1)
  })

  it('hiển thị trạng thái DOWN khi health endpoint trả về DOWN', async () => {
    api.getHealth.mockResolvedValue({
      data: {
        success: true,
        data: { status: 'DOWN', service: 'ecommerce-backend' },
        message: 'Service is healthy',
        errorCode: null,
        timestamp: '2026-01-01T00:00:00Z',
      },
    })

    render(<App />)

    await waitFor(() => {
      expect(screen.getByText(/Backend: DOWN/)).toBeInTheDocument()
    })
  })

  it('hiển thị lỗi khi không gọi được backend', async () => {
    api.getHealth.mockRejectedValue(new Error('Network Error'))

    render(<App />)

    await waitFor(() => {
      expect(screen.getByText('Backend DOWN')).toBeInTheDocument()
    })
  })
})
