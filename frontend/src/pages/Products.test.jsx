import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import api, {
  getCategories,
  getPublicProducts,
  getAdminProducts,
  getAdminProduct,
  deleteAdminCategory,
  getHealth,
} from '../api'

vi.mock('../api', () => ({
  getHealth: vi.fn(),
  getPublicProducts: vi.fn(),
  getPublicProduct: vi.fn(),
  getCategories: vi.fn(),
  getAdminProducts: vi.fn(),
  getAdminProduct: vi.fn(),
  createAdminProduct: vi.fn(),
  updateAdminProduct: vi.fn(),
  patchAdminProductStatus: vi.fn(),
  createAdminCategory: vi.fn(),
  updateAdminCategory: vi.fn(),
  deleteAdminCategory: vi.fn(),
  default: { get: vi.fn(), post: vi.fn() },
}))

const MOCK_USER = { id: 1, email: 'user@example.com', fullName: 'User Test', role: 'USER' }
const MOCK_ADMIN = { id: 2, email: 'admin@ecommerce.dev', fullName: 'System Admin', role: 'ADMIN' }

function pageResponse(content, overrides = {}) {
  return {
    data: {
      success: true,
      data: {
        content,
        page: 0,
        size: 20,
        totalElements: content.length,
        totalPages: 1,
        first: true,
        last: true,
        ...overrides,
      },
      message: 'ok',
      errorCode: null,
      timestamp: '2026-01-01T00:00:00Z',
    },
  }
}

function listResponse(items) {
  return {
    data: { success: true, data: items, message: 'ok', errorCode: null, timestamp: '2026-01-01T00:00:00Z' },
  }
}

function meResponse(user) {
  return {
    data: { success: true, data: user, message: 'ok', errorCode: null, timestamp: '2026-01-01T00:00:00Z' },
  }
}

const PRODUCTS = [
  {
    id: 1, sku: 'PHONE-001', name: 'Smartphone Nova X1', description: 'Man 6.5',
    price: 4990000, categoryId: 1, categoryName: 'dien thoai',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 2, sku: 'LAPTOP-001', name: 'Laptop Aero 14', description: 'Core i5',
    price: 15990000, categoryId: 2, categoryName: 'laptop',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
  },
]

const CATEGORIES = [
  { id: 1, name: 'dien thoai', description: 'Dien thoai' },
  { id: 2, name: 'laptop', description: 'Laptop' },
]

const ADMIN_PRODUCT = {
  id: 1, sku: 'PHONE-001', name: 'Smartphone Nova X1', description: 'Man 6.5',
  price: 4990000, status: 'ACTIVE', categoryId: 1, categoryName: 'dien thoai',
  stock: 30, version: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
}

function loginAs(user) {
  localStorage.setItem('accessToken', 'access-token')
  localStorage.setItem('refreshToken', 'refresh-token')
  localStorage.setItem('user', JSON.stringify(user))
  api.get.mockImplementation((url) => {
    if (url === '/api/v1/auth/me') return Promise.resolve(meResponse(user))
    return Promise.reject(new Error(`Unexpected GET ${url}`))
  })
}

beforeEach(() => {
  localStorage.clear()
  vi.clearAllMocks()
  getHealth.mockResolvedValue({
    data: { success: true, data: { status: 'UP', service: 'ecommerce-backend' } },
  })
})

describe('FE-PRD-01: ProductsPage render, đổi trang, filter danh mục', () => {
  it('render danh sách và filter categoryId gọi API đúng', async () => {
    window.history.pushState({}, '', '/products')
    getPublicProducts.mockResolvedValue(pageResponse(PRODUCTS))
    getCategories.mockResolvedValue(listResponse(CATEGORIES))

    render(<App />)

    expect(await screen.findByText('Smartphone Nova X1')).toBeInTheDocument()
    expect(screen.getByText('Laptop Aero 14')).toBeInTheDocument()

    getPublicProducts.mockClear()
    getPublicProducts.mockResolvedValue(pageResponse([PRODUCTS[1]]))

    // antd Select — mở dropdown qua .ant-select-selector rồi chọn option
    const categorySelect = screen.getByLabelText('Lọc danh mục')
    const selector = categorySelect.closest('.ant-select')?.querySelector('.ant-select-selector')
      || categorySelect.closest('div[class*="ant-select"]')
    fireEvent.mouseDown(selector)
    const laptopOption = await screen.findByTitle('laptop')
    fireEvent.click(laptopOption)

    await waitFor(() => {
      expect(getPublicProducts).toHaveBeenCalledWith(
        expect.objectContaining({ categoryId: 2, page: 0 }),
        expect.anything(),
      )
    })
  })
})

describe('FE-PRD-02: Search debounce 300ms', () => {
  it('gõ nhanh nhiều ký tự → API chỉ gọi sau khi ngừng gõ, search = chuỗi cuối', async () => {
    window.history.pushState({}, '', '/products')
    getPublicProducts.mockResolvedValue(pageResponse(PRODUCTS))
    getCategories.mockResolvedValue(listResponse(CATEGORIES))

    render(<App />)
    await screen.findByText('Smartphone Nova X1')

    getPublicProducts.mockClear()
    getPublicProducts.mockResolvedValue(pageResponse([]))

    const input = screen.getByLabelText('Tìm kiếm sản phẩm')
    fireEvent.change(input, { target: { value: 'n' } })
    fireEvent.change(input, { target: { value: 'no' } })
    fireEvent.change(input, { target: { value: 'nova' } })

    // Ngay sau khi gõ — debounce 300ms chưa nổ, không gọi API
    expect(getPublicProducts).not.toHaveBeenCalled()

    // Chờ debounce nổ + response
    await waitFor(
      () => {
        expect(getPublicProducts).toHaveBeenCalledTimes(1)
        expect(getPublicProducts).toHaveBeenCalledWith(
          expect.objectContaining({ search: 'nova' }),
          expect.anything(),
        )
      },
      { timeout: 2000 },
    )
  })
})

describe('FE-PRD-03: Route admin bị chặn với USER, render với ADMIN', () => {
  it('user USER vào /admin/products → redirect /', async () => {
    loginAs(MOCK_USER)
    window.history.pushState({}, '', '/admin/products')

    render(<App />)

    await waitFor(() => {
      expect(window.location.pathname).toBe('/')
    })
  })

  it('user ADMIN vào /admin/products → render table', async () => {
    loginAs(MOCK_ADMIN)
    window.history.pushState({}, '', '/admin/products')
    getAdminProducts.mockResolvedValue(
      pageResponse([
        {
          id: 1, sku: 'PHONE-001', name: 'Smartphone Nova X1', price: 4990000,
          status: 'ACTIVE', categoryName: 'dien thoai', stock: 30, version: 0,
        },
      ]),
    )
    getCategories.mockResolvedValue(listResponse(CATEGORIES))

    render(<App />)

    expect(await screen.findByText('PHONE-001')).toBeInTheDocument()
    expect(screen.getByText('Đang bán')).toBeInTheDocument()
  })
})

describe('FE-PRD-04: Đổi bộ lọc/search → page về 0', () => {
  it('chuyển trang rồi đổi search → request kế tiếp có page=0', async () => {
    window.history.pushState({}, '', '/products')
    getPublicProducts.mockResolvedValue(
      pageResponse(PRODUCTS, { page: 0, totalPages: 5, totalElements: 100 }),
    )
    getCategories.mockResolvedValue(listResponse(CATEGORIES))

    render(<App />)
    await screen.findByText('Smartphone Nova X1')

    // Chuyển sang trang 4 (page=3)
    getPublicProducts.mockClear()
    getPublicProducts.mockResolvedValue(
      pageResponse(PRODUCTS, { page: 3, totalPages: 5, totalElements: 100 }),
    )
    const pagination = document.querySelector('.ant-pagination')
    fireEvent.click(within(pagination).getByText('4'))

    await waitFor(() => {
      expect(getPublicProducts).toHaveBeenCalledWith(
        expect.objectContaining({ page: 3 }),
        expect.anything(),
      )
    })

    // Đổi search → page về 0
    getPublicProducts.mockClear()
    getPublicProducts.mockResolvedValue(pageResponse(PRODUCTS, { page: 0 }))

    fireEvent.change(screen.getByLabelText('Tìm kiếm sản phẩm'), {
      target: { value: 'a' },
    })

    await waitFor(
      () => {
        expect(getPublicProducts).toHaveBeenCalledWith(
          expect.objectContaining({ page: 0, search: 'a' }),
          expect.anything(),
        )
      },
      { timeout: 2000 },
    )
  })
})

describe('FE-PRD-05: Chống stale response — AbortController', () => {
  it('request cũ về muộn không ghi đè kết quả mới', async () => {
    window.history.pushState({}, '', '/products')

    let firstResolve
    const firstPromise = new Promise((resolve) => {
      firstResolve = resolve
    })

    getPublicProducts.mockImplementation((params) => {
      if (params.search === 'a') return firstPromise
      if (params.search === 'ab') return Promise.resolve(pageResponse([{ ...PRODUCTS[0], name: 'Ket qua moi nhat' }]))
      return Promise.resolve(pageResponse(PRODUCTS))
    })
    getCategories.mockResolvedValue(listResponse(CATEGORIES))

    render(<App />)
    await screen.findByText('Smartphone Nova X1')

    const input = screen.getByLabelText('Tìm kiếm sản phẩm')
    fireEvent.change(input, { target: { value: 'a' } })
    // Chờ debounce nổ → request 'a' gửi đi (promise giữ chưa resolve)
    await waitFor(() => {
      expect(getPublicProducts).toHaveBeenCalledWith(
        expect.objectContaining({ search: 'a' }),
        expect.anything(),
      )
    })

    // Gõ tiếp 'ab' → debounce nổ → request 'ab' resolve ngay
    fireEvent.change(input, { target: { value: 'ab' } })
    await screen.findByText('Ket qua moi nhat')

    // Request 'a' (cũ) về muộn — signal.aborted=true → không ghi đè UI
    firstResolve(pageResponse([{ ...PRODUCTS[0], name: 'Ket qua cu' }]))
    await new Promise((r) => setTimeout(r, 50))

    expect(screen.getByText('Ket qua moi nhat')).toBeInTheDocument()
    expect(screen.queryByText('Ket qua cu')).not.toBeInTheDocument()
  })
})

describe('FE-PRD-06: Form sửa SKU disabled; xóa danh mục lỗi CATEGORY_HAS_PRODUCTS', () => {
  it('mở form sửa → ô SKU disabled', async () => {
    loginAs(MOCK_ADMIN)
    window.history.pushState({}, '', '/admin/products')
    getAdminProducts.mockResolvedValue(
      pageResponse([
        {
          id: 1, sku: 'PHONE-001', name: 'Smartphone Nova X1', price: 4990000,
          status: 'ACTIVE', categoryName: 'dien thoai', stock: 30, version: 0,
        },
      ]),
    )
    getCategories.mockResolvedValue(listResponse(CATEGORIES))
    getAdminProduct.mockResolvedValue({
      data: { success: true, data: ADMIN_PRODUCT, message: 'ok', errorCode: null },
    })

    render(<App />)

    const editButton = await screen.findByRole('button', { name: 'Sửa' })
    fireEvent.click(editButton)

    await waitFor(() => {
      const skuInput = screen.getByPlaceholderText('PHONE-001')
      expect(skuInput).toBeDisabled()
    })
  })

  it('xóa danh mục lỗi CATEGORY_HAS_PRODUCTS → hiển thị message đúng', async () => {
    loginAs(MOCK_ADMIN)
    window.history.pushState({}, '', '/admin/categories')
    getCategories.mockResolvedValue(
      listResponse([{ id: 1, name: 'dien thoai', description: 'Dien thoai' }]),
    )
    deleteAdminCategory.mockRejectedValue({
      response: {
        data: {
          success: false,
          data: null,
          message: 'Category still has products; remove or reassign them first',
          errorCode: 'CATEGORY_HAS_PRODUCTS',
        },
      },
    })

    render(<App />)

    const deleteButton = await screen.findByRole('button', { name: /xóa/i })
    fireEvent.click(deleteButton)

    const confirmButton = await screen.findByRole('button', { name: /ok|có|xác nhận/i })
    fireEvent.click(confirmButton)

    await waitFor(() => {
      expect(
        screen.getByText('Category still has products; remove or reassign them first'),
      ).toBeInTheDocument()
    })
  })
})
