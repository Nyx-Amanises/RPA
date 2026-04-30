import { request } from './http'
import { mockCurrentUser, mockLogin } from '../mock/auth'

export function login(payload) {
  return request(
    {
      url: '/auth/login',
      method: 'post',
      data: payload
    },
    () => mockLogin(payload)
  )
}

export function fetchCurrentUser() {
  return request(
    {
      url: '/auth/current-user',
      method: 'get'
    },
    mockCurrentUser
  )
}
