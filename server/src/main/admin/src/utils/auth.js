const ADMIN_TOKEN_KEY = 'floating-ball-admin-token'
const ADMIN_USER_KEY = 'floating-ball-admin-user'
const ADMIN_EXPIRES_AT_KEY = 'floating-ball-admin-expires-at'

export const AUTH_CHANGE_EVENT = 'floating-ball-admin-auth-change'

function getStorage() {
  if (typeof window === 'undefined') {
    return null
  }
  return window.localStorage
}

function emitAuthChange() {
  if (typeof window === 'undefined') {
    return
  }
  window.dispatchEvent(new CustomEvent(AUTH_CHANGE_EVENT))
}

export function getAdminToken() {
  const storage = getStorage()
  return storage ? storage.getItem(ADMIN_TOKEN_KEY) || '' : ''
}

export function getAdminUser() {
  const storage = getStorage()
  if (!storage) {
    return null
  }
  const raw = storage.getItem(ADMIN_USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    storage.removeItem(ADMIN_USER_KEY)
    return null
  }
}

export function getAdminExpiresAt() {
  const storage = getStorage()
  if (!storage) {
    return ''
  }
  return storage.getItem(ADMIN_EXPIRES_AT_KEY) || ''
}

export function isAuthenticated() {
  return Boolean(getAdminToken())
}

export function setAdminUser(user) {
  const storage = getStorage()
  if (!storage) {
    return
  }
  if (user) {
    storage.setItem(ADMIN_USER_KEY, JSON.stringify(user))
  } else {
    storage.removeItem(ADMIN_USER_KEY)
  }
  emitAuthChange()
}

export function setAdminAuth(payload) {
  const storage = getStorage()
  if (!storage) {
    return
  }

  const token = payload && payload.token ? String(payload.token) : ''
  const expiresAt = payload && payload.expiresAt ? String(payload.expiresAt) : ''

  if (token) {
    storage.setItem(ADMIN_TOKEN_KEY, token)
  } else {
    storage.removeItem(ADMIN_TOKEN_KEY)
  }

  if (expiresAt) {
    storage.setItem(ADMIN_EXPIRES_AT_KEY, expiresAt)
  } else {
    storage.removeItem(ADMIN_EXPIRES_AT_KEY)
  }

  if (payload && payload.user) {
    storage.setItem(ADMIN_USER_KEY, JSON.stringify(payload.user))
  } else {
    storage.removeItem(ADMIN_USER_KEY)
  }

  emitAuthChange()
}

export function clearAdminAuth() {
  const storage = getStorage()
  if (!storage) {
    return
  }
  storage.removeItem(ADMIN_TOKEN_KEY)
  storage.removeItem(ADMIN_USER_KEY)
  storage.removeItem(ADMIN_EXPIRES_AT_KEY)
  emitAuthChange()
}
