import { getMockProfile } from './system'

export async function mockLogin(payload) {
  return {
    code: 100200,
    message: '登录成功',
    data: {
      token: `mock-token-${payload.username || 'admin'}`,
      tokenHead: 'Bearer ',
      userInfo: {
        id: 3,
        username: payload.username || 'admin',
        realName: '雪',
        roleNames: ['管理员']
      }
    }
  }
}

export async function mockCurrentUser() {
  return getMockProfile()
}
