import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
})

export function getHealth() {
  return api.get('/api/v1/health')
}

export default api
