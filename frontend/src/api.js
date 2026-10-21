import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
})

// Không gắn Bearer cho login/register/refresh — gửi kèm token cũ gây nhiễu và là nguyên nhân vòng lặp refresh (7.1)
const NO_BEARER_PATHS = [
  '/api/v1/auth/login',
  '/api/v1/auth/register',
  '/api/v1/auth/refresh',
]

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  const url = config.url || ''
  if (token && !NO_BEARER_PATHS.some((path) => url.startsWith(path))) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshPromise = null

export function clearSession() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  window.dispatchEvent(new Event('auth:session-updated'))
}

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.dispatchEvent(new CustomEvent('auth:navigate', { detail: '/login' }))
  }
}

/**
 * Refresh access token single-flight: mọi tab phải qua navigator.locks ('auth-refresh');
 * sau khi giành khóa đọc lại localStorage — nếu refresh token đã đổi nghĩa là tab khác
 * refresh xong, chỉ cần dùng token mới (đã chốt O-1-02 phương án (a)).
 */
function refreshAccessToken(usedRefreshToken) {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const doRefresh = async () => {
        const current = localStorage.getItem('refreshToken')
        if (!current) {
          throw new Error('Không có refresh token')
        }
        if (usedRefreshToken && current !== usedRefreshToken) {
          return localStorage.getItem('accessToken')
        }
        const response = await api.post('/api/v1/auth/refresh', { refreshToken: current })
        const data = response.data.data
        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)
        localStorage.setItem('user', JSON.stringify(data.user))
        window.dispatchEvent(new Event('auth:session-updated'))
        return data.accessToken
      }

      if (navigator.locks && navigator.locks.request) {
        return navigator.locks.request('auth-refresh', doRefresh)
      }
      return doRefresh()
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const response = error.response
    const config = error.config || {}
    if (!response || response.status !== 401) {
      return Promise.reject(error)
    }

    const url = config.url || ''
    const errorCode = response.data ? response.data.errorCode : null

    // 401 của login/register (INVALID_CREDENTIALS) là lỗi form — không clear, không redirect (chặn vòng lặp đăng nhập)
    if (url.includes('/auth/login') || url.includes('/auth/register')) {
      return Promise.reject(error)
    }

    // Request /auth/refresh bị 401 là kết thúc phiên — không retry chính nó
    if (url.includes('/auth/refresh')) {
      clearSession()
      redirectToLogin()
      return Promise.reject(error)
    }

    // TOKEN_EXPIRED → refresh một lần (single-flight) rồi retry request gốc
    if (errorCode === 'TOKEN_EXPIRED' && !config._retried) {
      config._retried = true
      try {
        const usedRefreshToken = localStorage.getItem('refreshToken')
        const newAccessToken = await refreshAccessToken(usedRefreshToken)
        config.headers.Authorization = `Bearer ${newAccessToken}`
        return api.request(config)
      } catch (refreshError) {
        clearSession()
        redirectToLogin()
        return Promise.reject(refreshError)
      }
    }

    // TOKEN_REVOKED / TOKEN_INVALID / các 401 khác → kết thúc phiên
    clearSession()
    redirectToLogin()
    return Promise.reject(error)
  },
)

export function getHealth() {
  return api.get('/api/v1/health')
}

// ---- Catalog (Phase 2) ----

export function getPublicProducts(params, config) {
  return api.get('/api/v1/products', { params, ...config })
}

export function getPublicProduct(id) {
  return api.get(`/api/v1/products/${id}`)
}

export function getCategories() {
  return api.get('/api/v1/categories')
}

export function getAdminProducts(params, config) {
  return api.get('/api/v1/admin/products', { params, ...config })
}

export function getAdminProduct(id) {
  return api.get(`/api/v1/admin/products/${id}`)
}

export function createAdminProduct(payload) {
  return api.post('/api/v1/admin/products', payload)
}

export function updateAdminProduct(id, payload) {
  return api.put(`/api/v1/admin/products/${id}`, payload)
}

export function patchAdminProductStatus(id, status) {
  return api.patch(`/api/v1/admin/products/${id}/status`, { status })
}

export function createAdminCategory(payload) {
  return api.post('/api/v1/admin/categories', payload)
}

export function updateAdminCategory(id, payload) {
  return api.put(`/api/v1/admin/categories/${id}`, payload)
}

export function deleteAdminCategory(id) {
  return api.delete(`/api/v1/admin/categories/${id}`)
}

export default api
