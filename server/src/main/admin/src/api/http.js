import axios from 'axios'
import { clearAdminAuth, getAdminToken } from '../utils/auth'

function resolveErrorMessage(error) {
  if (error && error.response && error.response.data && error.response.data.message) {
    return error.response.data.message
  }
  return (error && error.message) || '请求失败'
}

function isUnauthorizedCode(code) {
  const value = String(code || '').toLowerCase()
  return value === '401' || value === 'unauthorized'
}

function redirectToLogin() {
  if (typeof window === 'undefined') {
    return
  }
  const hash = window.location.hash || '#/overview'
  if (hash.indexOf('#/login') === 0) {
    return
  }
  const redirect = hash.replace(/^#/, '') || '/overview'
  window.location.replace(`#/login?redirect=${encodeURIComponent(redirect)}`)
}

function handleUnauthorized(message) {
  clearAdminAuth()
  redirectToLogin()
  return Promise.reject(new Error(message || '登录已失效，请重新登录'))
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

http.interceptors.request.use(config => {
  const token = getAdminToken()
  const url = config && config.url ? config.url : ''

  if (token && url !== '/admin/api/auth/login') {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

http.interceptors.response.use(
  response => {
    const body = response.data
    if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')) {
      if (String(body.code) === '0') {
        return body.data
      }
      if (isUnauthorizedCode(body.code)) {
        return handleUnauthorized(body.message)
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }

    return response.data
  },
  error => {
    if (error && error.response && error.response.status === 401) {
      return handleUnauthorized(resolveErrorMessage(error))
    }
    return Promise.reject(new Error(resolveErrorMessage(error)))
  }
)

export default http
