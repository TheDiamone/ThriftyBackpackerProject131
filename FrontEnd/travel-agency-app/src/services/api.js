import axios from 'axios'

const API_TIMEOUT_MS = Number(import.meta.env.VITE_API_TIMEOUT_MS || 20000)

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: Number.isFinite(API_TIMEOUT_MS) && API_TIMEOUT_MS > 0 ? API_TIMEOUT_MS : 20000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const detail = error.response?.data?.detail
    const detailMessage = Array.isArray(detail)
      ? detail
        .map((item) => {
          const location = Array.isArray(item?.loc) ? item.loc.join('.') : 'request'
          return `${location}: ${item?.msg || 'invalid value'}`
        })
        .join('; ')
      : typeof detail === 'string'
        ? detail
        : ''

    const message = detailMessage || error.response?.data?.message || error.message || 'An unexpected error occurred'
    return Promise.reject(new Error(message))
  }
)

export default api
