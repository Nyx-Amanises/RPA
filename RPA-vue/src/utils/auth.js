const TOKEN_KEY = 'rpa-token'
const USER_KEY = 'rpa-user'

export function getAuthToken() {
  const keys = [TOKEN_KEY, 'token', 'access_token', 'Authorization']
  for (const key of keys) {
    const value = window.localStorage.getItem(key)
    if (value) {
      return value
    }
  }
  return ''
}

export function setAuthToken(token) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

export function getAuthUser() {
  const raw = window.localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setAuthUser(user) {
  window.localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuthSession() {
  ;[TOKEN_KEY, USER_KEY, 'token', 'access_token', 'Authorization'].forEach((key) => {
    window.localStorage.removeItem(key)
  })
}
